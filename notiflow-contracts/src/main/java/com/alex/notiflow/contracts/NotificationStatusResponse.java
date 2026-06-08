package com.alex.notiflow.contracts;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationStatusResponse(
        UUID notificationId,
        NotificationChannel channel,
        String recipient,
        String subject,
        String message,
        Map<String, String> metadata,
        NotificationStatus status,
        int attempts,
        String lastFailureReason,
        Instant createdAt,
        Instant updatedAt
) {
}
