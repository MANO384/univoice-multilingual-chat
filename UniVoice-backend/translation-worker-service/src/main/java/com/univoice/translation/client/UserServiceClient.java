package com.univoice.translation.client;

import com.univoice.translation.dto.TranslationTarget;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(
            @Value("${app.services.user-service-url:https://univoice-multilingual-chat.onrender.com/api/v1/users}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl.trim()).build();
        log.info("UserServiceClient initialized -> baseUrl={}", baseUrl.trim());
    }

    public TranslationTarget resolveTarget(String receiverUuid, String senderUuid) {
        try {
            String targetLanguage = restClient.get()
                    .uri("/{receiverUuid}/target-language/{senderUuid}", receiverUuid, senderUuid)
                    .retrieve()
                    .body(String.class);

            if (targetLanguage == null || targetLanguage.isBlank() || "NONE".equalsIgnoreCase(targetLanguage)) {
                return new TranslationTarget(false, null);
            }
            return new TranslationTarget(true, targetLanguage.trim());
        } catch (Exception ex) {
            log.warn("Failed to resolve target language for receiver {}: {}. Defaulting to 'en'", receiverUuid, ex.getMessage());
            return new TranslationTarget(true, "en");
        }
    }
}