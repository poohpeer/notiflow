package com.alex.notiflow.relay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.alex.notiflow.contracts.NotificationStatus;
import com.alex.notiflow.relay.config.KafkaTopicsProperties;
import com.alex.notiflow.relay.domain.OutboxEventEntity;
import com.alex.notiflow.relay.repository.NotificationRepository;
import com.alex.notiflow.relay.repository.OutboxEventRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class OutboxPublisherTest {

    private OutboxEventRepository outboxEventRepository;
    private NotificationRepository notificationRepository;
    private KafkaTemplate<String, String> kafkaTemplate;
    private SimpleMeterRegistry meterRegistry;
    private OutboxPublisher publisher;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        outboxEventRepository = mock(OutboxEventRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        meterRegistry = new SimpleMeterRegistry();
        publisher = new OutboxPublisher(outboxEventRepository, notificationRepository, kafkaTemplate,
                new KafkaTopicsProperties("notiflow.notifications", "notiflow.notifications.dlq"), meterRegistry);
    }

    @Test
    void publishesPendingEventMarksItAndTransitionsNotificationToQueued() {
        var aggregateId = UUID.randomUUID();
        var event = pendingEvent(aggregateId);
        when(outboxEventRepository.lockPendingBatch(anyInt())).thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.<SendResult<String, String>>completedFuture(null));
        when(notificationRepository.updateStatusIfCurrentStatus(eq(aggregateId),
                eq(NotificationStatus.ACCEPTED), eq(NotificationStatus.QUEUED), any())).thenReturn(1);

        publisher.publishPending();

        verify(kafkaTemplate).send("notiflow.notifications", aggregateId.toString(), "{\"payload\":true}");
        assertThat(event.isPublished()).isTrue();
        assertThat(event.getPublishedAt()).isNotNull();
        verify(notificationRepository).updateStatusIfCurrentStatus(eq(aggregateId),
                eq(NotificationStatus.ACCEPTED), eq(NotificationStatus.QUEUED), any());
        assertThat(meterRegistry.counter("notiflow.outbox.published").count()).isEqualTo(1.0);
    }

    @Test
    void doesNothingWhenNoPendingEvents() {
        when(outboxEventRepository.lockPendingBatch(anyInt())).thenReturn(List.of());

        publisher.publishPending();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        assertThat(meterRegistry.counter("notiflow.outbox.published").count()).isEqualTo(0.0);
    }

    private OutboxEventEntity pendingEvent(UUID aggregateId) {
        var event = new OutboxEventEntity();
        event.setId(UUID.randomUUID());
        event.setAggregateType("notification");
        event.setAggregateId(aggregateId);
        event.setEventType("notification.created");
        event.setPayload("{\"payload\":true}");
        event.setPublished(false);
        event.setCreatedAt(Instant.now());
        return event;
    }
}
