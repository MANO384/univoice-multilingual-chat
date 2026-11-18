package com.univoice.chat.service;

import com.univoice.chat.dto.ChatDtos.ConversationPage;
import com.univoice.chat.dto.ChatDtos.MessageDto;
import com.univoice.chat.dto.IncomingMessageDto;
import com.univoice.chat.event.TranslatedMessageEvent;
import com.univoice.chat.security.ChatUser;

import java.util.Map;

public interface ChatService {
    MessageDto processIncomingMessage(ChatUser sender, IncomingMessageDto dto);
    void handleTranslatedMessage(TranslatedMessageEvent event);
    void markDelivered(String userUuid, String messageUuid);
    void markConversationRead(String readerUuid, String contactUuid);
    ConversationPage getConversation(String userUuid, String contactUuid, int page, int size);
    Map<String, Long> getUnreadCounts(String userUuid);
    boolean isUserOnline(String userUuid);
    MessageDto editMessage(String senderUuid, String messageUuid, String newContent);
    MessageDto deleteMessage(String senderUuid, String messageUuid);
}