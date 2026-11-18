package com.univoice.translation.event;

import java.time.LocalDateTime;

public record RawMessageEvent(
    String messageUuid,
    String senderUuid,
    String receiverUuid,
    String senderLanguage,
    String originalText,
    LocalDateTime timestamp
) {}
