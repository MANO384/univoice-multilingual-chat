package com.univoice.translation.event;

import java.time.LocalDateTime;
import java.util.List;

public record RawGroupMessageEvent(
        String messageUuid,
        String groupUuid,
        String senderUuid,
        String senderLanguage,
        String originalText,
        List<String> targetLanguages,
        LocalDateTime timestamp
) {}