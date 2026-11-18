package com.univoice.chat.kafka;

import com.univoice.chat.event.TranslatedMessageEvent;
import com.univoice.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatKafkaConsumer {

    private final ChatService chatService;

    @KafkaListener(topics = "${app.kafka.translated-messages-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeTranslatedMessage(TranslatedMessageEvent event) {
        log.info("Received translated event for uuid={}", event.messageUuid());
        chatService.handleTranslatedMessage(event);
    }
    
    private final com.univoice.chat.service.GroupChatService groupChatService;

    @KafkaListener(
        topics = "${app.kafka.translated-group-messages-topic:group-messages-translated}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeTranslatedGroupMessage(com.univoice.chat.event.TranslatedGroupMessageEvent event) {
        log.info("Received TranslatedGroupMessageEvent: groupUuid={}, messageUuid={}",
                event.groupUuid(), event.messageUuid());
        groupChatService.handleTranslatedGroupMessage(event);
    }
}
