package com.univoice.translation.kafka;

import com.univoice.translation.event.RawMessageEvent;
import com.univoice.translation.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TranslationKafkaConsumer {

    private final TranslationService translationService;

    @KafkaListener(topics = "${app.kafka.raw-messages-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRawMessage(RawMessageEvent event) {
        log.info("Consuming raw message: {}", event.messageUuid());
        translationService.processRawMessage(event);
    }
    
    @KafkaListener(
            topics = "${app.kafka.raw-group-messages-topic:group-messages-raw}",
            groupId = "${spring.kafka.consumer.group-id}"
        )
        public void consumeRawGroupMessage(com.univoice.translation.event.RawGroupMessageEvent event) {
            log.info("Consumed RawGroupMessageEvent: groupUuid={}, messageUuid={}, targets={}",
                    event.groupUuid(), event.messageUuid(), event.targetLanguages());
            translationService.processGroupMessage(event);
        }
}
