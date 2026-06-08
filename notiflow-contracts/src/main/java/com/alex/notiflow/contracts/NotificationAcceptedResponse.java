package com.alex.notiflow.contracts;

import java.util.UUID;

public record NotificationAcceptedResponse(
        UUID notificationId,
        NotificationStatus status,
        String statusUrl
) {
}
