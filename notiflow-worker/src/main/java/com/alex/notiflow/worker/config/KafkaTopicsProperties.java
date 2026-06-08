package com.alex.notiflow.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notiflow.kafka")
public record KafkaTopicsProperties(
        String notificationTopic,
        String dlqTopic
) {
}
