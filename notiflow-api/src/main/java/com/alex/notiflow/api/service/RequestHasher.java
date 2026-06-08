package com.alex.notiflow.api.service;

import com.alex.notiflow.contracts.NotificationRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

@Component
public class RequestHasher {
    private final ObjectMapper objectMapper;

    public RequestHasher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String hash(NotificationRequest request) {
        try {
            var canonical = new CanonicalRequest(
                    request.channel(),
                    request.recipient(),
                    request.subject(),
                    request.message(),
                    request.metadata() == null ? null : new TreeMap<>(request.metadata())
            );
            var bytes = objectMapper.writeValueAsBytes(canonical);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to hash notification request", exception);
        }
    }

    private record CanonicalRequest(
            Object channel,
            String recipient,
            String subject,
            String message,
            TreeMap<String, String> metadata
    ) {
    }
}
