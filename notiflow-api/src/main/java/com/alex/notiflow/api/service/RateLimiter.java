package com.alex.notiflow.api.service;

import com.alex.notiflow.api.config.RateLimitProperties;
import com.alex.notiflow.contracts.NotificationRequest;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class RateLimiter {
    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;

    public void check(NotificationRequest request) {
        var key = "notiflow:rate:%s:%s".formatted(request.channel(), request.recipient());
        Duration window = properties.window();
        redisTemplate.opsForValue().setIfAbsent(key, "0", window.toMillis(), TimeUnit.MILLISECONDS);
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count > properties.maxRequests()) {
            throw new RateLimitExceededException("Rate limit exceeded for channel and recipient");
        }
    }
}
