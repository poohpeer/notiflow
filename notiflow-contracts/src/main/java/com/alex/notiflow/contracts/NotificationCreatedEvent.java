package com.alex.notiflow.contracts;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationCreatedEvent(
        UUID notificationId,
        NotificationChannel channel,
        String recipient,
        String subject,
        String message,
        Map<String, String> metadata,
        Instant createdAt
) {
}
