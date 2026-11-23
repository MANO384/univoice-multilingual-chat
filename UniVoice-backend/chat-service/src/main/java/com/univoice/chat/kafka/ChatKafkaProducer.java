package com.univoice.chat.kafka;

import com.univoice.chat.event.RawGroupMessageEvent;
import com.univoice.chat.event.RawMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.raw-messages-topic}")
    private String rawMessagesTopic;

    @Value("${app.kafka.raw-group-messages-topic:group-messages-raw}")
    private String rawGroupMessagesTopic;

    @Value("${app.services.translation-worker-url:https://univoice-translation-worker.onrender.com}")
    private String translationWorkerUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final AtomicLong lastWakePingMs = new AtomicLong(0);

    public void publishRawMessage(RawMessageEvent event) {
        log.info("Emitting RawMessageEvent: uuid={}", event.messageUuid());
        kafkaTemplate.send(rawMessagesTopic, event.receiverUuid(), event);
        wakeTranslationWorkerIfNeeded();
    }

    public void publishRawGroupMessage(RawGroupMessageEvent event) {
        log.info("Emitting RawGroupMessageEvent: groupUuid={}, messageUuid={}, targets={}",
                event.groupUuid(), event.messageUuid(), event.targetLanguages());
        kafkaTemplate.send(rawGroupMessagesTopic, event.groupUuid(), event);
        wakeTranslationWorkerIfNeeded();
    }

    private void wakeTranslationWorkerIfNeeded() {
        if (translationWorkerUrl == null || translationWorkerUrl.isBlank()) return;
        long now = System.currentTimeMillis();
        long prev = lastWakePingMs.get();
        // Throttle wake-up pings to at most once every 5 minutes
        if (now - prev < 300_000L || !lastWakePingMs.compareAndSet(prev, now)) {
            return;
        }
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(translationWorkerUrl.trim()))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            httpClient.sendAsync(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
        }
    }
}