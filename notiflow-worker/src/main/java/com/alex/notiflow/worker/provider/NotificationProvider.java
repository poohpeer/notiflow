package com.alex.notiflow.worker.provider;

import com.alex.notiflow.contracts.NotificationChannel;
import com.alex.notiflow.contracts.NotificationCreatedEvent;
import com.alex.notiflow.contracts.ProviderResult;

public interface NotificationProvider {
    boolean supports(NotificationChannel channel);

    ProviderResult send(NotificationCreatedEvent event, int attempt);
}
