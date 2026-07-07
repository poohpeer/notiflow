package com.alex.notiflow.worker.service;

import java.time.Instant;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.alex.notiflow.contracts.FailureType;
import com.alex.notiflow.contracts.NotificationCreatedEvent;
import com.alex.notiflow.contracts.NotificationStatus;
import com.alex.notiflow.worker.config.KafkaTopicsProperties;
import com.alex.notiflow.worker.config.RetryProperties;
import com.alex.notiflow.worker.provider.ProviderRegistry;
import com.alex.notiflow.worker.repository.NotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationProcessor {

    private final NotificationRepository notificationRepository;
    private final ProviderRegistry providerRegistry;
    private final RetryProperties retryProperties;
    private final KafkaTopicsProperties topics;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public void process(NotificationCreatedEvent event) {
        var notification = notificationRepository.findById(event.notificationId())
                .orElseThrow(() -> new IllegalStateException("Notification was not found: " + event.notificationId()));
        if (isTerminal(notification.getStatus())) {
            log.info("Skipping already processed notification notificationId={} status={}", event.notificationId(),
                    notification.getStatus());
            return;
        }

        var provider = providerRegistry.get(event.channel());
        var maxAttempts = Math.max(1, retryProperties.maxAttempts());

        while (notification.getAttempts() < maxAttempts) {
            var attempt = notification.getAttempts() + 1;
            notification.setAttempts(attempt);
            notification.setStatus(NotificationStatus.PROCESSING);
            notification.setUpdatedAt(Instant.now());
            notificationRepository.saveAndFlush(notification);

            var result = provider.send(event, attempt);
            if (result.sent()) {
                notification.setStatus(NotificationStatus.SENT);
                notification.setLastFailureReason(null);
                notification.setUpdatedAt(Instant.now());
                notificationRepository.save(notification);
                meterRegistry.counter("notiflow.notifications.sent", "channel", event.channel().name()).increment();
                log.info("Notification sent notificationId={} channel={} attempt={}", event.notificationId(),
                        event.channel(), attempt);
                return;
            }

            notification.setLastFailureReason(result.reason());
            notification.setUpdatedAt(Instant.now());

            if (result.failureType() == FailureType.PERMANENT) {
                notification.setStatus(NotificationStatus.FAILED_PERMANENT);
                notificationRepository.save(notification);
                meterRegistry.counter("notiflow.notifications.failed", "channel", event.channel().name(), "type",
                        "permanent").increment();
                log.info("Notification failed permanently notificationId={} reason={}", event.notificationId(),
                        result.reason());
                return;
            }

            notification.setStatus(NotificationStatus.FAILED_RETRYABLE);
            notificationRepository.saveAndFlush(notification);
            meterRegistry.counter("notiflow.notifications.retry", "channel", event.channel().name()).increment();
            log.info("Notification retry scheduled notificationId={} attempt={} reason={}", event.notificationId(),
                    attempt, result.reason());
        }

        notification.setStatus(NotificationStatus.DEAD_LETTERED);
        notification.setUpdatedAt(Instant.now());
        notificationRepository.save(notification);
        publishToDlq(event);
        meterRegistry.counter("notiflow.notifications.dlq", "channel", event.channel().name()).increment();
        log.info("Notification dead-lettered notificationId={} attempts={}", event.notificationId(),
                notification.getAttempts());
    }

    private boolean isTerminal(NotificationStatus status) {
        return status == NotificationStatus.SENT || status == NotificationStatus.FAILED_PERMANENT
                || status == NotificationStatus.DEAD_LETTERED;
    }

    private void publishToDlq(NotificationCreatedEvent event) {
        try {
            kafkaTemplate.send(topics.dlqTopic(), event.notificationId().toString(),
                    objectMapper.writeValueAsString(event)).join();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize DLQ event", exception);
        }
    }
}
