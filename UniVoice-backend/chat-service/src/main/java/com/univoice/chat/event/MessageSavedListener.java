package com.univoice.chat.event;

import com.univoice.chat.dto.ChatDtos.MessageDto;
import com.univoice.chat.kafka.ChatKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageSavedListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectProvider<ChatKafkaProducer> producerProvider;

    @Value("${app.translation.enabled:true}")
    private boolean translationEnabled;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSaved(MessageSavedEvent event) {
        MessageDto message = event.message();

        try {
            messagingTemplate.convertAndSendToUser(message.receiverUuid(), "/queue/messages", message);
        } catch (Exception e) {
            log.warn("Could not push raw message to receiver: {}", e.getMessage());
        }

        if (!translationEnabled) return;

        try {
            producerProvider.ifAvailable(producer -> producer.publishRawMessage(new RawMessageEvent(
                    message.messageUuid(),
                    message.senderUuid(),
                    message.receiverUuid(),
                    event.senderLanguage(),
                    message.originalText(),
                    LocalDateTime.now())));
        } catch (Exception e) {
            log.error("Could not publish raw message to Kafka: {}", e.getMessage());
        }
    }
}
