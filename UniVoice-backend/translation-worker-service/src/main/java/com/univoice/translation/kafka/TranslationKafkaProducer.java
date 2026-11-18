package com.univoice.translation.kafka;

import com.univoice.translation.event.TranslatedMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TranslationKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.translated-messages-topic}")
    private String translatedTopic;

    public void publishTranslatedMessage(TranslatedMessageEvent event) {
        log.info("Emitting TranslatedMessageEvent to {}: messageUuid={}", translatedTopic, event.messageUuid());
        kafkaTemplate.send(translatedTopic, event.receiverUuid(), event);
    }
    
    @Value("${app.kafka.translated-group-messages-topic:group-messages-translated}")
    private String translatedGroupMessagesTopic;

    public void publishTranslatedGroupMessage(com.univoice.translation.event.TranslatedGroupMessageEvent event) {
        log.info("Publishing TranslatedGroupMessageEvent: groupUuid={}, messageUuid={}, languages={}",
                event.groupUuid(), event.messageUuid(), event.translations().keySet());
        kafkaTemplate.send(translatedGroupMessagesTopic, event.groupUuid(), event);
    }
}
