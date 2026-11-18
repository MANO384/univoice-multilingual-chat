package com.univoice.chat.kafka;

import com.univoice.chat.event.RawMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.raw-messages-topic}")
    private String rawMessagesTopic;

    public void publishRawMessage(RawMessageEvent event) {
        log.info("Emitting RawMessageEvent: uuid={}", event.messageUuid());
        kafkaTemplate.send(rawMessagesTopic, event.receiverUuid(), event);
    }
    
    @Value("${app.kafka.raw-group-messages-topic:group-messages-raw}")
    private String rawGroupMessagesTopic;

    public void publishRawGroupMessage(com.univoice.chat.event.RawGroupMessageEvent event) {
        log.info("Emitting RawGroupMessageEvent: groupUuid={}, messageUuid={}, targets={}",
                event.groupUuid(), event.messageUuid(), event.targetLanguages());
        kafkaTemplate.send(rawGroupMessagesTopic, event.groupUuid(), event);
    }
}
