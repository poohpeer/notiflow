package com.alex.notiflow.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notiflow.retry")
public record RetryProperties(
        int maxAttempts
) {
}
