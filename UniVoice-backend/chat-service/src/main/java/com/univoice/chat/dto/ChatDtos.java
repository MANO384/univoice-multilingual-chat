package com.univoice.chat.dto;

import com.univoice.chat.entity.ChatMessage;
import com.univoice.chat.entity.ChatMessage.MessageStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class ChatDtos {

    @Builder
    public record MessageDto(
            String messageUuid,
            String clientMsgId,
            String senderUuid,
            String receiverUuid,
            String originalText,
            String translatedText,
            String targetLanguage,
            MessageStatus status,
            boolean edited,
            boolean deleted,
            LocalDateTime createdAt
    ) {
        public static MessageDto from(ChatMessage entity) {
            return MessageDto.builder()
                    .messageUuid(entity.getMessageUuid())
                    .clientMsgId(entity.getClientMsgId())
                    .senderUuid(entity.getSenderUuid())
                    .receiverUuid(entity.getReceiverUuid())
                    .originalText(entity.isDeleted() ? "This message was deleted" : entity.getOriginalText())
                    .translatedText(entity.isDeleted() ? null : entity.getTranslatedText())
                    .targetLanguage(entity.getTargetLanguage())
                    .status(entity.getStatus())
                    .edited(entity.isEdited())
                    .deleted(entity.isDeleted())
                    .createdAt(entity.getCreatedAt())
                    .build();
        }
    }

    public record EditMessageDto(String messageUuid, String newContent) {}
    public record MessageRefDto(String messageUuid) {}
    public record ContactRefDto(String contactUuid) {}
    public record StatusUpdateDto(List<String> messageUuids, String status) {}
    public record ErrorDto(String code, String message) {}
    public record ConversationPage(List<MessageDto> messages, boolean hasNext) {}
}