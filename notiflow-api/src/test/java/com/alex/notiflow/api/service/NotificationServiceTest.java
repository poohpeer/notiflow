package com.alex.notiflow.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.alex.notiflow.api.domain.NotificationEntity;
import com.alex.notiflow.api.domain.OutboxEventEntity;
import com.alex.notiflow.api.repository.NotificationRepository;
import com.alex.notiflow.api.repository.OutboxEventRepository;
import com.alex.notiflow.contracts.NotificationChannel;
import com.alex.notiflow.contracts.NotificationRequest;
import com.alex.notiflow.contracts.NotificationStatus;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private OutboxEventRepository outboxEventRepository;
    private RateLimiter rateLimiter;
    private RequestHasher requestHasher;
    private NotificationMapper mapper;
    private SimpleMeterRegistry meterRegistry;
    private NotificationService service;

    private final NotificationRequest request = new NotificationRequest(
            NotificationChannel.EMAIL, "user@example.com", "Welcome", "Hello", Map.of("k", "v"));

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        outboxEventRepository = mock(OutboxEventRepository.class);
        rateLimiter = mock(RateLimiter.class);
        requestHasher = mock(RequestHasher.class);
        mapper = mock(NotificationMapper.class);
        meterRegistry = new SimpleMeterRegistry();
        service = new NotificationService(notificationRepository, outboxEventRepository, rateLimiter,
                requestHasher, mapper, meterRegistry);
    }

    @Test
    void persistsNotificationAndOutboxEventInThesameCall() {
        when(requestHasher.hash(request)).thenReturn("hash");
        when(notificationRepository.findByIdempotencyKey("key")).thenReturn(Optional.empty());
        when(mapper.writeMetadata(request.metadata())).thenReturn("{\"k\":\"v\"}");
        when(mapper.toOutboxEvent(any(), any(), any())).thenReturn(new OutboxEventEntity());

        var response = service.create("key", request);

        assertThat(response.status()).isEqualTo(NotificationStatus.ACCEPTED);
        assertThat(response.notificationId()).isNotNull();
        assertThat(response.statusUrl()).isEqualTo("/api/v1/notifications/" + response.notificationId());
        verify(rateLimiter).check(request);
        verify(notificationRepository).save(any(NotificationEntity.class));
        verify(outboxEventRepository).save(any(OutboxEventEntity.class));
        assertThat(meterRegistry.counter("notiflow.notifications.accepted", "channel", "EMAIL").count())
                .isEqualTo(1.0);
    }

    @Test
    void returnsExistingNotificationWhenSameKeyAndSamePayload() {
        var existing = existingNotification("hash");
        when(requestHasher.hash(request)).thenReturn("hash");
        when(notificationRepository.findByIdempotencyKey("key")).thenReturn(Optional.of(existing));

        var response = service.create("key", request);

        assertThat(response.notificationId()).isEqualTo(existing.getId());
        verify(rateLimiter, never()).check(any());
        verify(notificationRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void rejectsSameKeyWithDifferentPayload() {
        when(requestHasher.hash(request)).thenReturn("new-hash");
        when(notificationRepository.findByIdempotencyKey("key"))
                .thenReturn(Optional.of(existingNotification("old-hash")));

        assertThatThrownBy(() -> service.create("key", request))
                .isInstanceOf(IdempotencyConflictException.class);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void rejectsMissingIdempotencyKey() {
        assertThatThrownBy(() -> service.create("  ", request))
                .isInstanceOf(MissingIdempotencyKeyException.class);
        verify(notificationRepository, never()).findByIdempotencyKey(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void doesNotPersistWhenRateLimitExceeded() {
        when(requestHasher.hash(request)).thenReturn("hash");
        when(notificationRepository.findByIdempotencyKey("key")).thenReturn(Optional.empty());
        doThrow(new RateLimitExceededException("limit")).when(rateLimiter).check(request);

        assertThatThrownBy(() -> service.create("key", request))
                .isInstanceOf(RateLimitExceededException.class);
        verify(notificationRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    private NotificationEntity existingNotification(String hash) {
        var entity = new NotificationEntity();
        entity.setId(UUID.randomUUID());
        entity.setRequestHash(hash);
        entity.setStatus(NotificationStatus.ACCEPTED);
        return entity;
    }
}
