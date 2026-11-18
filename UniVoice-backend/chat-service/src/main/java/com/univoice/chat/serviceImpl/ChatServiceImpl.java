package com.univoice.chat.serviceImpl;

import com.univoice.chat.dto.ChatDtos.ConversationPage;
import com.univoice.chat.dto.ChatDtos.MessageDto;
import com.univoice.chat.dto.ChatDtos.StatusUpdateDto;
import com.univoice.chat.dto.IncomingMessageDto;
import com.univoice.chat.entity.ChatMessage;
import com.univoice.chat.entity.ChatMessage.MessageStatus;
import com.univoice.chat.event.MessageSavedEvent;
import com.univoice.chat.event.TranslatedMessageEvent;
import com.univoice.chat.repository.ChatMessageRepository;
import com.univoice.chat.security.ChatUser;
import com.univoice.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ChatMessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final SimpUserRegistry simpUserRegistry;

    @Override
    @Transactional
    public MessageDto processIncomingMessage(ChatUser sender, IncomingMessageDto dto) {
        String senderUuid = sender.getUserUuid();

        if (senderUuid.equals(dto.getReceiverUuid())) {
            throw new IllegalArgumentException("Cannot send message to yourself");
        }

        String clientMsgId = dto.getClientMsgId();
        if (clientMsgId != null && !clientMsgId.isBlank()) {
            Optional<ChatMessage> existing = messageRepository.findBySenderUuidAndClientMsgId(senderUuid, clientMsgId);
            if (existing.isPresent()) {
                return MessageDto.from(existing.get());
            }
        }

        ChatMessage saved = messageRepository.save(ChatMessage.builder()
                .messageUuid(UUID.randomUUID().toString())
                .clientMsgId(clientMsgId == null || clientMsgId.isBlank() ? null : clientMsgId)
                .senderUuid(senderUuid)
                .receiverUuid(dto.getReceiverUuid())
                .originalText(dto.getContent().trim())
                .status(MessageStatus.SENT)
                .build());

        MessageDto result = MessageDto.from(saved);
        eventPublisher.publishEvent(new MessageSavedEvent(result, sender.getMotherTongue()));
        return result;
    }

    @Override
    @Transactional
    public void handleTranslatedMessage(TranslatedMessageEvent event) {
        Optional<ChatMessage> found = messageRepository.findByMessageUuid(event.messageUuid());
        if (found.isEmpty()) return;

        ChatMessage message = found.get();
        message.setTranslatedText(event.translatedText());
        message.setTargetLanguage(event.targetLanguage());
        messageRepository.save(message);

        messagingTemplate.convertAndSendToUser(message.getReceiverUuid(), "/queue/messages", MessageDto.from(message));
    }

    @Override
    @Transactional
    public void markDelivered(String userUuid, String messageUuid) {
        messageRepository.findByMessageUuid(messageUuid)
                .filter(m -> m.getReceiverUuid().equals(userUuid))
                .filter(m -> m.getStatus() == MessageStatus.SENT)
                .ifPresent(m -> {
                    m.setStatus(MessageStatus.DELIVERED);
                    messageRepository.save(m);
                    messagingTemplate.convertAndSendToUser(m.getSenderUuid(), "/queue/status",
                            new StatusUpdateDto(List.of(m.getMessageUuid()), MessageStatus.DELIVERED.name()));
                });
    }

    @Override
    @Transactional
    public void markConversationRead(String readerUuid, String contactUuid) {
        List<ChatMessage> unread = messageRepository
                .findByReceiverUuidAndSenderUuidAndStatusNot(readerUuid, contactUuid, MessageStatus.READ);
        if (unread.isEmpty()) return;

        unread.forEach(m -> m.setStatus(MessageStatus.READ));
        messageRepository.saveAll(unread);

        List<String> ids = unread.stream().map(ChatMessage::getMessageUuid).toList();
        messagingTemplate.convertAndSendToUser(contactUuid, "/queue/status",
                new StatusUpdateDto(ids, MessageStatus.READ.name()));
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationPage getConversation(String userUuid, String contactUuid, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        int safePage = Math.max(0, page);

        Slice<ChatMessage> slice = messageRepository.findConversation(userUuid, contactUuid,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id")));

        List<MessageDto> messages = new ArrayList<>(slice.getContent().stream().map(MessageDto::from).toList());
        Collections.reverse(messages);
        return new ConversationPage(messages, slice.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getUnreadCounts(String userUuid) {
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : messageRepository.countUnreadBySender(userUuid, MessageStatus.READ)) {
            counts.put((String) row[0], (Long) row[1]);
        }
        return counts;
    }
    
    
    @Override
    public boolean isUserOnline(String userUuid) {
        return simpUserRegistry.getUser(userUuid) != null;
    }

    @Override
    @Transactional
    public MessageDto editMessage(String senderUuid, String messageUuid, String newContent) {
        ChatMessage msg = messageRepository.findByMessageUuid(messageUuid)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        if (!msg.getSenderUuid().equals(senderUuid)) {
            throw new IllegalArgumentException("You can only edit your own messages");
        }
        if (msg.isDeleted()) {
            throw new IllegalArgumentException("Cannot edit a deleted message");
        }

        msg.setOriginalText(newContent.trim());
        msg.setTranslatedText(null);
        msg.setEdited(true);
        ChatMessage saved = messageRepository.save(msg);

        MessageDto dto = MessageDto.from(saved);
        messagingTemplate.convertAndSendToUser(msg.getSenderUuid(), "/queue/messages", dto);
        messagingTemplate.convertAndSendToUser(msg.getReceiverUuid(), "/queue/messages", dto);

        // Re-trigger translation for the edited text
        eventPublisher.publishEvent(new MessageSavedEvent(dto, "auto"));
        return dto;
    }

    @Override
    @Transactional
    public MessageDto deleteMessage(String senderUuid, String messageUuid) {
        ChatMessage msg = messageRepository.findByMessageUuid(messageUuid)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        if (!msg.getSenderUuid().equals(senderUuid)) {
            throw new IllegalArgumentException("You can only delete your own messages");
        }

        msg.setDeleted(true);
        msg.setOriginalText("This message was deleted");
        msg.setTranslatedText(null);
        ChatMessage saved = messageRepository.save(msg);

        MessageDto dto = MessageDto.from(saved);
        messagingTemplate.convertAndSendToUser(msg.getSenderUuid(), "/queue/messages", dto);
        messagingTemplate.convertAndSendToUser(msg.getReceiverUuid(), "/queue/messages", dto);
        return dto;
    }
}
