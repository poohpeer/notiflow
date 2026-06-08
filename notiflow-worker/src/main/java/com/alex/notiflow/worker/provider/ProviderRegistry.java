package com.alex.notiflow.worker.provider;

import com.alex.notiflow.contracts.NotificationChannel;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProviderRegistry {
    private final List<NotificationProvider> providers;

    public ProviderRegistry(List<NotificationProvider> providers) {
        this.providers = providers;
    }

    public NotificationProvider get(NotificationChannel channel) {
        return providers.stream()
                .filter(provider -> provider.supports(channel))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No provider registered for channel " + channel));
    }
}
