package com.univoice.chat.event;

import com.univoice.chat.dto.ChatDtos.MessageDto;

public record MessageSavedEvent(
    MessageDto message,
    String senderLanguage
) {}
