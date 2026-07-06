package com.alex.notiflow.api.service;

import com.alex.notiflow.api.domain.NotificationEntity;
import com.alex.notiflow.api.domain.OutboxEventEntity;
import com.alex.notiflow.contracts.NotificationCreatedEvent;
import com.alex.notiflow.contracts.NotificationStatusResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class NotificationMapper {
    private static final TypeReference<Map<String, String>> METADATA_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper mapper;

    public NotificationStatusResponse toStatusResponse(NotificationEntity entity) {
        return new NotificationStatusResponse(
                entity.getId(),
                entity.getChannel(),
                entity.getRecipient(),
                entity.getSubject(),
                entity.getMessage(),
                readMetadata(entity.getMetadataJson()),
                entity.getStatus(),
                entity.getAttempts(),
                entity.getLastFailureReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public OutboxEventEntity toOutboxEvent(NotificationEntity notification, Map<String, String> metadata, Instant now) {
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
            outboxEvent.setPayload(mapper.writeValueAsString(event));
            outboxEvent.setPublished(false);
            outboxEvent.setCreatedAt(now);
            return outboxEvent;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize notification event", exception);
        }
    }

    public String writeMetadata(Map<String, String> metadata) {
        try {
            return metadata == null ? null : mapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid notification metadata", exception);
        }
    }

    private Map<String, String> readMetadata(String metadataJson) {
        try {
            return metadataJson == null ? Map.of() : mapper.readValue(metadataJson, METADATA_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored notification metadata is invalid", exception);
        }
    }
}
