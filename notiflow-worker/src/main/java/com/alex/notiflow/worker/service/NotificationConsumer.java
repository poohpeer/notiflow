package com.alex.notiflow.worker.service;

import com.alex.notiflow.contracts.NotificationCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {
    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final ObjectMapper objectMapper;
    private final NotificationProcessor processor;

    public NotificationConsumer(ObjectMapper objectMapper, NotificationProcessor processor) {
        this.objectMapper = objectMapper;
        this.processor = processor;
    }

    @KafkaListener(
            topics = "${notiflow.kafka.notification-topic}",
            groupId = "${spring.kafka.consumer.group-id:notiflow-worker}"
    )
    public void consume(String payload) throws JsonProcessingException {
        var event = objectMapper.readValue(payload, NotificationCreatedEvent.class);
        log.info("Consumed notification event notificationId={} channel={}", event.notificationId(), event.channel());
        processor.process(event);
    }
}
