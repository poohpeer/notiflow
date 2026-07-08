package com.alex.notiflow.api.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.alex.notiflow.api.service.IdempotencyConflictException;
import com.alex.notiflow.api.service.MissingIdempotencyKeyException;
import com.alex.notiflow.api.service.NotificationNotFoundException;
import com.alex.notiflow.api.service.NotificationService;
import com.alex.notiflow.api.service.RateLimitExceededException;
import com.alex.notiflow.contracts.NotificationAcceptedResponse;
import com.alex.notiflow.contracts.NotificationStatus;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    private static final String VALID_BODY = """
            {"channel":"EMAIL","recipient":"user@example.com","subject":"Welcome","message":"Hello","metadata":{}}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void acceptsValidRequestAndReturns202() throws Exception {
        var id = UUID.randomUUID();
        when(notificationService.create(eq("key-1"), any()))
                .thenReturn(new NotificationAcceptedResponse(id, NotificationStatus.ACCEPTED,
                        "/api/v1/notifications/" + id));

        mockMvc.perform(post("/api/v1/notifications")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.notificationId").value(id.toString()))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.statusUrl").value("/api/v1/notifications/" + id));
    }

    @Test
    void returns400WhenIdempotencyKeyMissing() throws Exception {
        when(notificationService.create(isNull(), any())).thenThrow(new MissingIdempotencyKeyException());

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void returns400OnValidationFailureWithoutCallingService() throws Exception {
        var invalidBody = """
                {"recipient":"user@example.com","message":"Hello"}
                """;

        mockMvc.perform(post("/api/v1/notifications")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(notificationService);
    }

    @Test
    void returns409OnIdempotencyConflict() throws Exception {
        when(notificationService.create(any(), any()))
                .thenThrow(new IdempotencyConflictException("payload differs"));

        mockMvc.perform(post("/api/v1/notifications")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void returns429WhenRateLimited() throws Exception {
        when(notificationService.create(any(), any()))
                .thenThrow(new RateLimitExceededException("too many"));

        mockMvc.perform(post("/api/v1/notifications")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void returnsNotificationById() throws Exception {
        var id = UUID.randomUUID();
        when(notificationService.get(id)).thenReturn(null);

        mockMvc.perform(get("/api/v1/notifications/{id}", id))
                .andExpect(status().isOk());
    }

    @Test
    void returns404WhenNotificationMissing() throws Exception {
        var id = UUID.randomUUID();
        when(notificationService.get(id)).thenThrow(new NotificationNotFoundException(id));

        mockMvc.perform(get("/api/v1/notifications/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
