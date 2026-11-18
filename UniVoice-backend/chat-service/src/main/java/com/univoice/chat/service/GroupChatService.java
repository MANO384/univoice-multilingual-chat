package com.univoice.chat.service;

import com.univoice.chat.dto.GroupDtos.*;
import com.univoice.chat.event.TranslatedGroupMessageEvent;
import com.univoice.chat.security.ChatUser;

public interface GroupChatService {

    GroupMessageDto sendGroupMessage(ChatUser sender, SendGroupMessageRequest request);

    void handleTranslatedGroupMessage(TranslatedGroupMessageEvent event);

    GroupConversationPage getGroupMessages(String requesterUuid, String groupUuid, int page, int size);

    void editGroupMessage(String senderUuid, EditGroupMessageRequest request);

    void deleteGroupMessage(String senderUuid, DeleteGroupMessageRequest request);
}