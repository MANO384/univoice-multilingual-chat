package com.univoice.translation.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GlobalTranslationClient {

    private static final Map<String, String> LANG_NAMES = Map.ofEntries(
            Map.entry("en", "English"),
            Map.entry("es", "Spanish"),
            Map.entry("fr", "French"),
            Map.entry("de", "German"),
            Map.entry("ja", "Japanese"),
            Map.entry("ko", "Korean"),
            Map.entry("ar", "Arabic"),
            Map.entry("zh", "Chinese (Simplified)"),
            Map.entry("pt", "Portuguese"),
            Map.entry("ru", "Russian"),
            Map.entry("it", "Italian"),
            Map.entry("tr", "Turkish"),
            Map.entry("vi", "Vietnamese"),
            Map.entry("th", "Thai"),
            Map.entry("id", "Indonesian"),
            Map.entry("hi", "Hindi"),
            Map.entry("ta", "Tamil"),
            Map.entry("te", "Telugu"),
            Map.entry("kn", "Kannada"),
            Map.entry("ml", "Malayalam"),
            Map.entry("bn", "Bengali"),
            Map.entry("mr", "Marathi"),
            Map.entry("gu", "Gujarati"),
            Map.entry("pa", "Punjabi"),
            Map.entry("od", "Odia")
    );

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GlobalTranslationClient(
            @Value("${app.global-translate.api-url:https://api.groq.com/openai/v1}") String apiUrl,
            @Value("${app.global-translate.api-key:dummy}") String apiKey,
            @Value("${app.global-translate.model:llama-3.3-70b-versatile}") String model) {
        this.apiKey = apiKey != null ? apiKey.trim() : "dummy";
        this.model = model != null ? model.trim() : "llama-3.3-70b-versatile";
        this.restClient = RestClient.builder().baseUrl(apiUrl.trim()).build();
        log.info("GlobalTranslationClient initialized -> url={}, model={}, keyConfigured={}",
                apiUrl, this.model, !"dummy".equals(this.apiKey) && !this.apiKey.isBlank());
    }

    public String translate(String text, String targetLangCode) {
        if ("dummy".equals(apiKey) || apiKey.isBlank()) {
            log.error("Global translation API key is 'dummy' or blank! Check app.global-translate.api-key in application.properties");
            return text;
        }

        String targetLanguageName = LANG_NAMES.getOrDefault(targetLangCode.toLowerCase(), targetLangCode);

        try {
            Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.2,
                "messages", List.of(
                    Map.of("role", "system", "content",
                        "You are a real-time chat translator. Translate the user's message naturally into " +
                        targetLanguageName + ". Return ONLY the translated text without quotes, notes, or explanations."),
                    Map.of("role", "user", "content", text)
                )
            );

            Map<?, ?> response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.get("choices") instanceof List<?> choices && !choices.isEmpty()) {
                Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
                Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
                if (message != null && message.get("content") != null) {
                    String translated = message.get("content").toString().trim();
                    log.info("Global AI translated '{}' -> '{}' ({})", text, translated, targetLanguageName);
                    return translated;
                }
            }
        } catch (Exception ex) {
            log.error("Global translation API error for '{}' -> {}: {}", text, targetLanguageName, ex.getMessage());
        }
        return text;
    }
}