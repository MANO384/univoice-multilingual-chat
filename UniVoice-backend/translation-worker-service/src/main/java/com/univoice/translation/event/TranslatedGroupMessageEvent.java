package com.univoice.translation.event;

import java.time.LocalDateTime;
import java.util.Map;

public record TranslatedGroupMessageEvent(
        String messageUuid,
        String groupUuid,
        String senderUuid,
        String originalText,
        Map<String, String> translations,
        LocalDateTime timestamp
) {}