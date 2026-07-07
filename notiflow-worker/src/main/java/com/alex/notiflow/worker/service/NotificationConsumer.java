package com.alex.notiflow.worker.service;

import com.alex.notiflow.contracts.NotificationCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationProcessor processor;

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
