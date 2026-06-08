package com.alex.notiflow.api.service;

import java.util.UUID;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException(UUID id) {
        super("Notification %s was not found".formatted(id));
    }
}
