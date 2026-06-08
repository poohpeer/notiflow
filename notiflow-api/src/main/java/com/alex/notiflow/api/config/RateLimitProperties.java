package com.alex.notiflow.api.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notiflow.rate-limit")
public record RateLimitProperties(
        int maxRequests,
        Duration window
) {
}
