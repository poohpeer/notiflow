package com.alex.notiflow.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.alex.notiflow.contracts.NotificationChannel;
import com.alex.notiflow.contracts.NotificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RequestHasherTest {
    private final RequestHasher requestHasher = new RequestHasher(new ObjectMapper());

    @Test
    void hashesMetadataIndependentlyFromMapInsertionOrder() {
        var first = new NotificationRequest(
                NotificationChannel.EMAIL,
                "user@example.com",
                "Welcome",
                "Hello",
                Map.of("b", "2", "a", "1")
        );
        var second = new NotificationRequest(
                NotificationChannel.EMAIL,
                "user@example.com",
                "Welcome",
                "Hello",
                Map.of("a", "1", "b", "2")
        );

        assertThat(requestHasher.hash(first)).isEqualTo(requestHasher.hash(second));
    }

    @Test
    void changesHashWhenPayloadChanges() {
        var first = new NotificationRequest(NotificationChannel.SMS, "+123456", null, "Code 1234", Map.of());
        var second = new NotificationRequest(NotificationChannel.SMS, "+123456", null, "Code 5678", Map.of());

        assertThat(requestHasher.hash(first)).isNotEqualTo(requestHasher.hash(second));
    }
}
