package com.univoice.translation.client;

import com.univoice.translation.dto.SarvamTranslationDto;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class SarvamAiClient {

    private final RestClient restClient;
    private final String model;

    @Getter
    private final String mode;

    public SarvamAiClient(
            @Value("${app.sarvam.api-url}") String apiUrl,
            @Value("${app.sarvam.api-key}") String apiKey,
            @Value("${app.sarvam.model:mayura:v1}") String model,
            @Value("${app.sarvam.mode:formal}") String mode) {
        this.model = model;
        this.mode = mode;
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("api-subscription-key", apiKey)
                .build();
    }

    public String translate(String text, String sourceLocale, String targetLocale) {
        try {
            SarvamTranslationDto.Request request = new SarvamTranslationDto.Request(
                    text,
                    sourceLocale != null && !sourceLocale.isBlank() ? sourceLocale : "auto",
                    targetLocale,
                    mode,
                    model
            );

            SarvamTranslationDto.Response response = restClient.post()
                    .uri("/translate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(SarvamTranslationDto.Response.class);

            if (response != null && response.translatedText() != null && !response.translatedText().isBlank()) {
                return response.translatedText();
            }
        } catch (Exception ex) {
            log.warn("Sarvam AI translation unavailable for '{}': {}. Falling back to original text.", text, ex.getMessage());
        }
        return text;
    }
}
