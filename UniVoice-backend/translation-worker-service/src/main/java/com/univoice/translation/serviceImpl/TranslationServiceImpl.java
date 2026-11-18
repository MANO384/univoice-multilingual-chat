package com.univoice.translation.serviceImpl;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.univoice.translation.client.GlobalTranslationClient;
import com.univoice.translation.client.SarvamAiClient;
import com.univoice.translation.client.UserServiceClient;
import com.univoice.translation.dto.TranslationTarget;
import com.univoice.translation.entity.MessageTranslation;
import com.univoice.translation.event.RawGroupMessageEvent;
import com.univoice.translation.event.RawMessageEvent;
import com.univoice.translation.event.TranslatedGroupMessageEvent;
import com.univoice.translation.event.TranslatedMessageEvent;
import com.univoice.translation.kafka.TranslationKafkaProducer;
import com.univoice.translation.repository.MessageTranslationRepository;
import com.univoice.translation.service.LanguageCodes;
import com.univoice.translation.service.TranslationCacheService;
import com.univoice.translation.service.TranslationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationServiceImpl implements TranslationService {

    private final SarvamAiClient sarvamAiClient;
    private final GlobalTranslationClient globalTranslationClient;
    private final UserServiceClient userServiceClient;
    private final TranslationCacheService cacheService;
    private final MessageTranslationRepository translationRepository;
    private final TranslationKafkaProducer kafkaProducer;

    @Override
    public void processRawMessage(RawMessageEvent event) {
        TranslationTarget target = userServiceClient.resolveTarget(event.receiverUuid(), event.senderUuid());
        if (!target.translate() || target.targetLanguage() == null || "NONE".equalsIgnoreCase(target.targetLanguage())) {
            log.info("Translation disabled (Unchanged) for receiver={} from sender={}. Skipping translation.",
                    event.receiverUuid(), event.senderUuid());
            return;
        }

        String targetLang = LanguageCodes.base(target.targetLanguage());
        String sourceLang = LanguageCodes.base(event.senderLanguage());

        if (targetLang.equals(sourceLang)) {
            return;
        }

        String translated = translationRepository
                .findByMessageUuidAndTargetLanguage(event.messageUuid(), targetLang)
                .map(MessageTranslation::getTranslatedText)
                .orElseGet(() -> translateAndStore(event, sourceLang, targetLang));

        kafkaProducer.publishTranslatedMessage(new TranslatedMessageEvent(
                event.messageUuid(),
                event.senderUuid(),
                event.receiverUuid(),
                event.originalText(),
                translated,
                targetLang,
                LocalDateTime.now()));
    }

    @Override
    public void processGroupMessage(RawGroupMessageEvent event) {
        if (event == null || event.originalText() == null || event.originalText().isBlank()) return;
        if (event.targetLanguages() == null || event.targetLanguages().isEmpty()) return;

        String sourceLang = LanguageCodes.base(event.senderLanguage());
        Map<String, String> translations = new LinkedHashMap<>();

        for (String rawTarget : event.targetLanguages()) {
            if (rawTarget == null || rawTarget.isBlank() || "NONE".equalsIgnoreCase(rawTarget)) continue;
            String targetLang = LanguageCodes.base(rawTarget);
            if (targetLang.equals(sourceLang)) continue;

            String translatedText = translateHybrid(event.originalText(), sourceLang, targetLang);
            if (translatedText != null && !translatedText.isBlank() && !translatedText.equals(event.originalText())) {
                translations.put(targetLang, translatedText);
            }
        }

        if (!translations.isEmpty()) {
            kafkaProducer.publishTranslatedGroupMessage(new TranslatedGroupMessageEvent(
                    event.messageUuid(),
                    event.groupUuid(),
                    event.senderUuid(),
                    event.originalText(),
                    translations,
                    LocalDateTime.now()
            ));
        }
    }

    private String translateAndStore(RawMessageEvent event, String sourceLang, String targetLang) {
        String translated = translateHybrid(event.originalText(), sourceLang, targetLang);

        try {
            translationRepository.save(MessageTranslation.builder()
                    .messageUuid(event.messageUuid())
                    .targetLanguage(targetLang)
                    .translatedText(translated)
                    .translationProvider("HYBRID_SARVAM_GLOBAL")
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.debug("Concurrent duplicate translation ignored: {}", event.messageUuid());
        }
        return translated;
    }

    private String translateHybrid(String text, String sourceLang, String targetLang) {
        String cacheKeySource = sourceLang == null ? "auto" : sourceLang;
        String mode = sarvamAiClient.getMode();

        return cacheService.get(text, cacheKeySource, targetLang, mode)
                .orElseGet(() -> {
                    String result;
                    boolean sourceInSarvam = LanguageCodes.isSarvamSupported(sourceLang) || "auto".equals(sourceLang);
                    boolean targetInSarvam = LanguageCodes.isSarvamSupported(targetLang);

                    if (sourceInSarvam && targetInSarvam) {
                        // Case 1: Indian <-> Indian or Indian <-> English -> Sarvam AI
                        String srcLocale = LanguageCodes.toSarvam(sourceLang).orElse("auto");
                        String tgtLocale = LanguageCodes.toSarvam(targetLang).orElse("en-IN");
                        result = sarvamAiClient.translate(text, srcLocale, tgtLocale);
                    } else if (LanguageCodes.isIndic(sourceLang) && !targetInSarvam) {
                        // Case 2: Indian (e.g., Tanglish) -> Foreign (e.g., German/Chinese) via English pivot
                        String srcLocale = LanguageCodes.toSarvam(sourceLang).orElse("auto");
                        String englishPivot = sarvamAiClient.translate(text, srcLocale, "en-IN");
                        result = globalTranslationClient.translate(englishPivot, targetLang);
                    } else if (!sourceInSarvam && LanguageCodes.isIndic(targetLang) && !"en".equals(targetLang)) {
                        // Case 3a: Foreign (e.g., German) -> English pivot -> Indian via Sarvam AI
                        String englishPivot = "en".equals(sourceLang) ? text : globalTranslationClient.translate(text, "en");
                        String tgtLocale = LanguageCodes.toSarvam(targetLang).orElse("en-IN");
                        result = sarvamAiClient.translate(englishPivot, "en-IN", tgtLocale);
                    } else {
                        // Case 3b: Foreign <-> Foreign (e.g., German -> Chinese, German -> Spanish)
                        result = globalTranslationClient.translate(text, targetLang);
                    }

                    if (result != null && !result.isBlank() && !result.equals(text)) {
                        cacheService.put(text, cacheKeySource, targetLang, mode, result);
                    }
                    return result;
                });
    }
}