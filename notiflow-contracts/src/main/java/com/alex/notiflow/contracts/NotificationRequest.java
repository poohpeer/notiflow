package com.alex.notiflow.contracts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record NotificationRequest(
        @NotNull NotificationChannel channel,
        @NotBlank @Size(max = 320) String recipient,
        @Size(max = 200) String subject,
        @NotBlank @Size(max = 4_000) String message,
        Map<String, String> metadata
) {
}
