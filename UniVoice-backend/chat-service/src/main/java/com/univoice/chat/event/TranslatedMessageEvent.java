package com.univoice.chat.event;

import java.time.LocalDateTime;

public record TranslatedMessageEvent(
    String messageUuid,
    String senderUuid,
    String receiverUuid,
    String originalText,
    String translatedText,
    String targetLanguage,
    LocalDateTime timestamp
) {}
