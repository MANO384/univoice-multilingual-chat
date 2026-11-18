package com.univoice.translation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SarvamTranslationDto {

    public record Request(
        String input,
        @JsonProperty("source_language_code") String sourceLanguageCode,
        @JsonProperty("target_language_code") String targetLanguageCode,
        String mode,
        String model
    ) {}

    public record Response(
        @JsonProperty("translated_text") String translatedText
    ) {}
}
