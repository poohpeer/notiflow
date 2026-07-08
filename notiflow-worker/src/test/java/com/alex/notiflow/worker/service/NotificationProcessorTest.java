package com.alex.notiflow.worker.service;

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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.alex.notiflow.contracts.NotificationChannel;
import com.alex.notiflow.contracts.NotificationCreatedEvent;
import com.alex.notiflow.contracts.NotificationStatus;
import com.alex.notiflow.contracts.ProviderResult;
import com.alex.notiflow.worker.config.KafkaTopicsProperties;
import com.alex.notiflow.worker.config.RetryProperties;
import com.alex.notiflow.worker.domain.NotificationEntity;
import com.alex.notiflow.worker.provider.NotificationProvider;
import com.alex.notiflow.worker.provider.ProviderRegistry;
import com.alex.notiflow.worker.repository.NotificationRepository;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class NotificationProcessorTest {

    private static final UUID ID = UUID.randomUUID();

    private NotificationRepository notificationRepository;
    private ProviderRegistry providerRegistry;
    private NotificationProvider provider;
    private KafkaTemplate<String, String> kafkaTemplate;
    private SimpleMeterRegistry meterRegistry;
    private NotificationProcessor processor;

    private final NotificationCreatedEvent event = new NotificationCreatedEvent(
            ID, NotificationChannel.EMAIL, "user@example.com", "Subject", "Message", Map.of(), Instant.now());

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        providerRegistry = mock(ProviderRegistry.class);
        provider = mock(NotificationProvider.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        meterRegistry = new SimpleMeterRegistry();
        when(providerRegistry.get(NotificationChannel.EMAIL)).thenReturn(provider);
        processor = new NotificationProcessor(notificationRepository, providerRegistry,
                new RetryProperties(3), new KafkaTopicsProperties("notiflow.notifications", "notiflow.notifications.dlq"),
                kafkaTemplate, JsonMapper.builder().addModule(new JavaTimeModule()).build(), meterRegistry);
    }

    @Test
    void marksSentOnFirstSuccessfulAttempt() {
        var notification = notification(NotificationStatus.ACCEPTED, 0);
        when(notificationRepository.findById(ID)).thenReturn(Optional.of(notification));
        when(provider.send(event, 1)).thenReturn(ProviderResult.success());

        processor.process(event);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getAttempts()).isEqualTo(1);
        assertThat(meterRegistry.counter("notiflow.notifications.sent", "channel", "EMAIL").count()).isEqualTo(1.0);
    }

    @Test
    void marksFailedPermanentWithoutRetrying() {
        var notification = notification(NotificationStatus.ACCEPTED, 0);
        when(notificationRepository.findById(ID)).thenReturn(Optional.of(notification));
        when(provider.send(event, 1)).thenReturn(ProviderResult.permanent("bad recipient"));

        processor.process(event);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED_PERMANENT);
        assertThat(notification.getAttempts()).isEqualTo(1);
        verify(provider).send(event, 1);
        verify(provider, never()).send(event, 2);
    }

    @Test
    void retriesThenSends() {
        var notification = notification(NotificationStatus.ACCEPTED, 0);
        when(notificationRepository.findById(ID)).thenReturn(Optional.of(notification));
        when(provider.send(event, 1)).thenReturn(ProviderResult.retryable("temporary"));
        when(provider.send(event, 2)).thenReturn(ProviderResult.success());

        processor.process(event);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getAttempts()).isEqualTo(2);
        assertThat(meterRegistry.counter("notiflow.notifications.retry", "channel", "EMAIL").count()).isEqualTo(1.0);
    }

    @Test
    void deadLettersAndPublishesToDlqAfterExhaustingAttempts() {
        var notification = notification(NotificationStatus.ACCEPTED, 0);
        when(notificationRepository.findById(ID)).thenReturn(Optional.of(notification));
        when(provider.send(eq(event), anyInt())).thenReturn(ProviderResult.retryable("temporary"));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.<SendResult<String, String>>completedFuture(null));

        processor.process(event);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.DEAD_LETTERED);
        assertThat(notification.getAttempts()).isEqualTo(3);
        verify(kafkaTemplate).send(eq("notiflow.notifications.dlq"), eq(ID.toString()), anyString());
        assertThat(meterRegistry.counter("notiflow.notifications.dlq", "channel", "EMAIL").count()).isEqualTo(1.0);
    }

    @Test
    void skipsAlreadyTerminalNotification() {
        var notification = notification(NotificationStatus.SENT, 1);
        when(notificationRepository.findById(ID)).thenReturn(Optional.of(notification));

        processor.process(event);

        verify(providerRegistry, never()).get(any());
        verify(notificationRepository, never()).save(any());
    }

    private NotificationEntity notification(NotificationStatus status, int attempts) {
        var entity = new NotificationEntity();
        entity.setId(ID);
        entity.setChannel(NotificationChannel.EMAIL);
        entity.setStatus(status);
        entity.setAttempts(attempts);
        return entity;
    }
}
