package com.alex.notiflow.worker.provider;

import com.alex.notiflow.contracts.NotificationChannel;
import com.alex.notiflow.contracts.NotificationCreatedEvent;
import com.alex.notiflow.contracts.ProviderResult;
import java.util.EnumSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MockNotificationProvider implements NotificationProvider {

    @Override
    public boolean supports(NotificationChannel channel) {
        return EnumSet.allOf(NotificationChannel.class).contains(channel);
    }

    @Override
    public ProviderResult send(NotificationCreatedEvent event, int attempt) {
        var mode = event.metadata() == null ? null : event.metadata().get("mockFailure");
        log.info(
                "Mock send notificationId={} channel={} recipient={} attempt={}",
                event.notificationId(),
                event.channel(),
                event.recipient(),
                attempt
        );

        if ("permanent".equalsIgnoreCase(mode)) {
            return ProviderResult.permanent("Mock permanent provider failure");
        }
        if ("retryable".equalsIgnoreCase(mode)) {
            return ProviderResult.retryable("Mock retryable provider failure");
        }
        if ("retryable-once".equalsIgnoreCase(mode) && attempt == 1) {
            return ProviderResult.retryable("Mock retryable provider failure on first attempt");
        }
        return ProviderResult.success();
    }
}
