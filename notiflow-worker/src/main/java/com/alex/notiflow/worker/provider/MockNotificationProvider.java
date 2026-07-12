package com.alex.notiflow.worker.provider;

import static com.alex.notiflow.contracts.MockFailureType.PERMANENT;
import static com.alex.notiflow.contracts.MockFailureType.RETRYABLE;
import static com.alex.notiflow.contracts.MockFailureType.RETRYABLE_ONCE;

import java.util.EnumSet;

import org.springframework.stereotype.Component;

import com.alex.notiflow.contracts.MockFailureType;
import com.alex.notiflow.contracts.NotificationChannel;
import com.alex.notiflow.contracts.NotificationCreatedEvent;
import com.alex.notiflow.contracts.ProviderResult;

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
        var mode = MockFailureType.getRandomFailure();
        log.info("Mock send notificationId={} channel={} recipient={} attempt={}", event.notificationId(),
                event.channel(), event.recipient(), attempt);

        if (PERMANENT.equals(mode)) {
            return ProviderResult.permanent("Mock permanent provider failure");
        }
        if (RETRYABLE.equals(mode)) {
            return ProviderResult.retryable("Mock retryable provider failure");
        }
        if (RETRYABLE_ONCE.equals(mode) && attempt == 1) {
            return ProviderResult.retryable("Mock retryable provider failure on first attempt");
        }
        return ProviderResult.success();
    }

}
