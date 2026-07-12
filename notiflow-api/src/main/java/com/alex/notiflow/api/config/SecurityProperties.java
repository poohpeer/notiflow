package com.alex.notiflow.api.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The single dashboard user. {@code passwordHash} is a bcrypt hash, never a
 * plaintext password — generate one with
 * {@code htpasswd -bnBC 12 "" 'secret' | tr -d ':\n'}.
 */
@ConfigurationProperties(prefix = "notiflow.security")
public record SecurityProperties(
        String username,
        String passwordHash,
        Duration sessionTimeout
) {
}
