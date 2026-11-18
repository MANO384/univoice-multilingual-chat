package com.univoice.chat.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.raw-group-messages-topic:group-messages-raw}")
    private String rawGroupMessagesTopic;

    @Value("${app.kafka.translated-group-messages-topic:group-messages-translated}")
    private String translatedGroupMessagesTopic;

    @Bean
    public NewTopic rawGroupMessagesTopic() {
        return TopicBuilder.name(rawGroupMessagesTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic translatedGroupMessagesTopic() {
        return TopicBuilder.name(translatedGroupMessagesTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}