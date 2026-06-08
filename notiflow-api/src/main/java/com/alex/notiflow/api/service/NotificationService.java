package com.alex.notiflow.api.service;

import com.alex.notiflow.api.domain.NotificationEntity;
import com.alex.notiflow.api.domain.OutboxEventEntity;
import com.alex.notiflow.api.repository.NotificationRepository;
import com.alex.notiflow.api.repository.OutboxEventRepository;
import com.alex.notiflow.contracts.NotificationAcceptedResponse;
import com.alex.notiflow.contracts.NotificationChannel;
import com.alex.notiflow.contracts.NotificationCreatedEvent;
import com.alex.notiflow.contracts.NotificationRequest;
import com.alex.notiflow.contracts.NotificationStatus;
import com.alex.notiflow.contracts.NotificationStatusResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final RateLimiter rateLimiter;
    private final RequestHasher requestHasher;
    private final NotificationMapper mapper;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public NotificationService(
            NotificationRepository notificationRepository,
            OutboxEventRepository outboxEventRepository,
            RateLimiter rateLimiter,
            RequestHasher requestHasher,
            NotificationMapper mapper,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry
    ) {
        this.notificationRepository = notificationRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.rateLimiter = rateLimiter;
        this.requestHasher = requestHasher;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public NotificationAcceptedResponse create(String idempotencyKey, NotificationRequest request) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new MissingIdempotencyKeyException();
        }

        var requestHash = requestHasher.hash(request);
        var existing = notificationRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            var notification = existing.get();
            if (!notification.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException("Idempotency-Key was already used with a different payload");
            }
            return acceptedResponse(notification);
        }

        rateLimiter.check(request);

        var now = Instant.now();
        var notification = new NotificationEntity();
        notification.setId(UUID.randomUUID());
        notification.setIdempotencyKey(idempotencyKey);
        notification.setRequestHash(requestHash);
        notification.setChannel(request.channel());
        notification.setRecipient(request.recipient());
        notification.setSubject(request.subject());
        notification.setMessage(request.message());
        notification.setMetadataJson(mapper.writeMetadata(request.metadata()));
        notification.setStatus(NotificationStatus.ACCEPTED);
        notification.setAttempts(0);
        notification.setCreatedAt(now);
        notification.setUpdatedAt(now);

        notificationRepository.save(notification);
        outboxEventRepository.save(toOutboxEvent(notification, request.metadata(), now));
        meterRegistry.counter("notiflow.notifications.accepted", "channel", request.channel().name()).increment();
        return acceptedResponse(notification);
    }

    public NotificationStatusResponse get(UUID id) {
        return notificationRepository.findById(id)
                .map(mapper::toStatusResponse)
                .orElseThrow(() -> new NotificationNotFoundException(id));
    }

    public Page<NotificationStatusResponse> list(
            NotificationChannel channel,
            NotificationStatus status,
            Pageable pageable
    ) {
        Page<NotificationEntity> page;
        if (channel != null && status != null) {
            page = notificationRepository.findByChannelAndStatus(channel, status, pageable);
        } else if (channel != null) {
            page = notificationRepository.findByChannel(channel, pageable);
        } else if (status != null) {
            page = notificationRepository.findByStatus(status, pageable);
        } else {
            page = notificationRepository.findAll(pageable);
        }
        return page.map(mapper::toStatusResponse);
    }

    private OutboxEventEntity toOutboxEvent(NotificationEntity notification, Map<String, String> metadata, Instant now) {
        try {
            var event = new NotificationCreatedEvent(
                    notification.getId(),
                    notification.getChannel(),
                    notification.getRecipient(),
                    notification.getSubject(),
                    notification.getMessage(),
                    metadata,
                    now
            );
            var outboxEvent = new OutboxEventEntity();
            outboxEvent.setAggregateType("notification");
            outboxEvent.setAggregateId(notification.getId());
            outboxEvent.setEventType("notification.created");
            outboxEvent.setPayload(objectMapper.writeValueAsString(event));
            outboxEvent.setPublished(false);
            outboxEvent.setCreatedAt(now);
            return outboxEvent;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize notification event", exception);
        }
    }

    private NotificationAcceptedResponse acceptedResponse(NotificationEntity notification) {
        return new NotificationAcceptedResponse(
                notification.getId(),
                notification.getStatus(),
                "/api/v1/notifications/" + notification.getId()
        );
    }
}
