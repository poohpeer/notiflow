package com.alex.notiflow.contracts;

public enum NotificationStatus {
    ACCEPTED,
    QUEUED,
    PROCESSING,
    SENT,
    FAILED_RETRYABLE,
    FAILED_PERMANENT,
    DEAD_LETTERED
}
