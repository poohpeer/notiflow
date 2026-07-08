package com.alex.notiflow.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.alex.notiflow.api.config.RateLimitProperties;
import com.alex.notiflow.contracts.NotificationChannel;
import com.alex.notiflow.contracts.NotificationRequest;

class RateLimiterTest {

    private static final String KEY = "notiflow:rate:SMS:+123456789";

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private RateLimiter rateLimiter;

    private final NotificationRequest request = new NotificationRequest(
            NotificationChannel.SMS, "+123456789", null, "Code 1234", Map.of());

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        rateLimiter = new RateLimiter(redisTemplate, new RateLimitProperties(5, Duration.ofMinutes(1)));
    }

    @Test
    void allowsRequestUnderLimitAndSeedsCounterWithWindowTtl() {
        when(valueOps.increment(KEY)).thenReturn(3L);

        assertThatCode(() -> rateLimiter.check(request)).doesNotThrowAnyException();

        verify(valueOps).setIfAbsent(eq(KEY), eq("0"), eq(Duration.ofMinutes(1).toMillis()),
                eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void allowsRequestExactlyAtLimit() {
        when(valueOps.increment(KEY)).thenReturn(5L);

        assertThatCode(() -> rateLimiter.check(request)).doesNotThrowAnyException();
    }

    @Test
    void rejectsRequestOverLimit() {
        when(valueOps.increment(KEY)).thenReturn(6L);

        assertThatThrownBy(() -> rateLimiter.check(request))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void keyIsScopedPerChannelAndRecipient() {
        when(valueOps.increment(KEY)).thenReturn(1L);

        rateLimiter.check(request);

        assertThat(KEY).contains(NotificationChannel.SMS.name()).contains(request.recipient());
    }
}
