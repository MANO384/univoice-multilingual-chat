package com.univoice.translation.dto;

public record TranslationTarget(
    boolean translate,
    String targetLanguage
) {}
