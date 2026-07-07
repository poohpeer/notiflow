package com.alex.notiflow.worker.provider;

import com.alex.notiflow.contracts.NotificationChannel;
import java.util.List;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class ProviderRegistry {
    private final List<NotificationProvider> providers;

    public NotificationProvider get(NotificationChannel channel) {
        return providers.stream()
                .filter(provider -> provider.supports(channel))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No provider registered for channel " + channel));
    }
}
