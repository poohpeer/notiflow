package com.alex.notiflow.relay.service;

import com.alex.notiflow.contracts.NotificationStatus;
import com.alex.notiflow.relay.config.KafkaTopicsProperties;
import com.alex.notiflow.relay.repository.NotificationRepository;
import com.alex.notiflow.relay.repository.OutboxEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.transaction.Transactional;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository outboxEventRepository;
    private final NotificationRepository notificationRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicsProperties topics;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelayString = "${notiflow.outbox.publish-delay:PT2S}")
    @Transactional
    public void publishPending() {
        var events = outboxEventRepository.lockPendingBatch(BATCH_SIZE);
        for (var event : events) {
            kafkaTemplate.send(topics.notificationTopic(), event.getAggregateId().toString(), event.getPayload()).join();
            event.setPublished(true);
            event.setPublishedAt(Instant.now());
            var queuedUpdates = notificationRepository.updateStatusIfCurrentStatus(
                    event.getAggregateId(),
                    NotificationStatus.ACCEPTED,
                    NotificationStatus.QUEUED,
                    Instant.now()
            );
            if (queuedUpdates == 0) {
                log.debug("Skipped QUEUED transition for outbox event id={} notificationId={} because status already changed",
                        event.getId(), event.getAggregateId());
            }
            meterRegistry.counter("notiflow.outbox.published").increment();
            log.info("Published outbox event id={} notificationId={}", event.getId(), event.getAggregateId());
        }
    }
}
