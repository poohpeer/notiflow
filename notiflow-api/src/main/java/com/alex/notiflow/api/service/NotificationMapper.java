package com.alex.notiflow.api.service;

import com.alex.notiflow.api.domain.NotificationEntity;
import com.alex.notiflow.contracts.NotificationStatusResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {
    private static final TypeReference<Map<String, String>> METADATA_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public NotificationMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

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

    public String writeMetadata(Map<String, String> metadata) {
        try {
            return metadata == null ? null : objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid notification metadata", exception);
        }
    }

    private Map<String, String> readMetadata(String metadataJson) {
        try {
            return metadataJson == null ? Map.of() : objectMapper.readValue(metadataJson, METADATA_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored notification metadata is invalid", exception);
        }
    }
}
