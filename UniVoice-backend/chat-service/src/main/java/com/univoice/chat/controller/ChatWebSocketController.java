package com.univoice.chat.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import com.univoice.chat.dto.ChatDtos.ContactRefDto;
import com.univoice.chat.dto.ChatDtos.EditMessageDto;
import com.univoice.chat.dto.ChatDtos.ErrorDto;
import com.univoice.chat.dto.ChatDtos.MessageDto;
import com.univoice.chat.dto.ChatDtos.MessageRefDto;
import com.univoice.chat.dto.IncomingMessageDto;
import com.univoice.chat.security.ChatUser;
import com.univoice.chat.service.ChatService;
import com.univoice.chat.service.GroupChatService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final GroupChatService groupChatService;

    @MessageMapping("/chat.send")
    @SendToUser("/queue/ack")
    public MessageDto send(@Payload @Validated IncomingMessageDto dto, Principal principal) {
        if (principal instanceof ChatUser chatUser) {
            return chatService.processIncomingMessage(chatUser, dto);
        }
        throw new IllegalArgumentException("Not authenticated");
    }

    @MessageMapping("/chat.delivered")
    public void delivered(@Payload @Validated MessageRefDto dto, Principal principal) {
        chatService.markDelivered(principal.getName(), dto.messageUuid());
    }

    @MessageMapping("/chat.read")
    public void read(@Payload @Validated ContactRefDto dto, Principal principal) {
        chatService.markConversationRead(principal.getName(), dto.contactUuid());
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public ErrorDto handleError(Exception ex) {
        return new ErrorDto("ERROR", ex.getMessage());
    }
    
    @MessageMapping("/chat.edit")
    public void edit(@Payload EditMessageDto dto, Principal principal) {
        chatService.editMessage(principal.getName(), dto.messageUuid(), dto.newContent());
    }

    @MessageMapping("/chat.delete")
    public void delete(@Payload MessageRefDto dto, Principal principal) {
        chatService.deleteMessage(principal.getName(), dto.messageUuid());
    }
    

    @MessageMapping("/group.send")
    public void sendGroup(@Payload @Validated com.univoice.chat.dto.GroupDtos.SendGroupMessageRequest dto, Principal principal) {
        if (principal instanceof ChatUser chatUser) {
            groupChatService.sendGroupMessage(chatUser, dto);
            return;
        }
        throw new IllegalArgumentException("Not authenticated");
    }

    @MessageMapping("/group.edit")
    public void editGroup(@Payload @Validated com.univoice.chat.dto.GroupDtos.EditGroupMessageRequest dto, Principal principal) {
        groupChatService.editGroupMessage(principal.getName(), dto);
    }

    @MessageMapping("/group.delete")
    public void deleteGroup(@Payload @Validated com.univoice.chat.dto.GroupDtos.DeleteGroupMessageRequest dto, Principal principal) {
        groupChatService.deleteGroupMessage(principal.getName(), dto);
    }
}
