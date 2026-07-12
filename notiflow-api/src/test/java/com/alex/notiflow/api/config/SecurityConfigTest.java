package com.alex.notiflow.api.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.alex.notiflow.api.service.NotificationService;
import com.alex.notiflow.api.web.AuthController;
import com.alex.notiflow.api.web.NotificationController;
import com.alex.notiflow.contracts.NotificationAcceptedResponse;
import com.alex.notiflow.contracts.NotificationStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Guards the auth rules themselves. {@link com.alex.notiflow.api.web.NotificationControllerTest}
 * deliberately runs without a security chain (@WebMvcTest does not load
 * {@link SecurityConfig}), so without this test nothing would notice the api
 * going wide open.
 *
 * <p>{@link SessionConfig} stays out — it needs a live Redis; the filter chain
 * under test does not care where the session is stored.
 */
@WebMvcTest(controllers = { NotificationController.class, AuthController.class })
@Import({ SecurityConfig.class, SecurityConfigTest.TestConfig.class })
@TestPropertySource(properties = {
        "notiflow.security.username=demo",
        // bcrypt("secret"), cost 4 — cheap on purpose so the test stays fast.
        "notiflow.security.password-hash=$2y$04$gwepwmsA3..tOx1FVvYrTeopI3uSLEWzQR9OJKXMvTR83KenYU4RK",
        "notiflow.security.session-timeout=PT8H",
})
class SecurityConfigTest {

    private static final String VALID_BODY = """
            {"channel":"EMAIL","recipient":"user@example.com","subject":"Welcome","message":"Hello","metadata":{}}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Test
    void anonymousReadIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void anonymousWriteIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/notifications").with(csrf())
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedReadIsAllowed() throws Exception {
        var id = UUID.randomUUID();
        when(notificationService.get(id)).thenReturn(null);

        mockMvc.perform(get("/api/v1/notifications/{id}", id).with(user("demo")))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedWriteIsAllowed() throws Exception {
        var id = UUID.randomUUID();
        when(notificationService.create(eq("key-1"), any()))
                .thenReturn(new NotificationAcceptedResponse(id, NotificationStatus.ACCEPTED,
                        "/api/v1/notifications/" + id));

        mockMvc.perform(post("/api/v1/notifications").with(user("demo")).with(csrf())
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isAccepted());
    }

    @Test
    void writeWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/notifications").with(user("demo"))
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginWithValidCredentialsSucceeds() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"demo","password":"secret"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("demo"));
    }

    @Test
    void loginWithWrongPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"demo","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
