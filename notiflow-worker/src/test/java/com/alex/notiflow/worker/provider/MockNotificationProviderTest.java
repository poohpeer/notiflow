package com.alex.notiflow.worker.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.alex.notiflow.contracts.FailureType;
import com.alex.notiflow.contracts.NotificationChannel;
import com.alex.notiflow.contracts.NotificationCreatedEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MockNotificationProviderTest {
    private final MockNotificationProvider provider = new MockNotificationProvider();

    @Test
    void supportsEveryMvpChannel() {
        assertThat(provider.supports(NotificationChannel.EMAIL)).isTrue();
        assertThat(provider.supports(NotificationChannel.TELEGRAM)).isTrue();
        assertThat(provider.supports(NotificationChannel.SMS)).isTrue();
        assertThat(provider.supports(NotificationChannel.PUSH)).isTrue();
    }

    @Test
    void returnsPermanentFailureWhenRequestedByMetadata() {
        var result = provider.send(event(Map.of("mockFailure", "permanent")), 1);

        assertThat(result.sent()).isFalse();
        assertThat(result.failureType()).isEqualTo(FailureType.PERMANENT);
    }

    @Test
    void retryableOnceSucceedsAfterFirstAttempt() {
        var event = event(Map.of("mockFailure", "retryable_once"));

        assertThat(provider.send(event, 1).failureType()).isEqualTo(FailureType.RETRYABLE);
        assertThat(provider.send(event, 2).sent()).isTrue();
    }

    private NotificationCreatedEvent event(Map<String, String> metadata) {
        return new NotificationCreatedEvent(
                UUID.randomUUID(),
                NotificationChannel.EMAIL,
                "user@example.com",
                "Subject",
                "Message",
                metadata,
                Instant.now()
        );
    }
}
