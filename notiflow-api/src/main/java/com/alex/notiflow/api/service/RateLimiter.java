package com.alex.notiflow.api.service;

import com.alex.notiflow.api.config.RateLimitProperties;
import com.alex.notiflow.contracts.NotificationRequest;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RateLimiter {
    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;

    public RateLimiter(StringRedisTemplate redisTemplate, RateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public void check(NotificationRequest request) {
        var key = "notiflow:rate:%s:%s".formatted(request.channel(), request.recipient());
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            Duration window = properties.window();
            redisTemplate.expire(key, window);
        }
        if (count != null && count > properties.maxRequests()) {
            throw new RateLimitExceededException("Rate limit exceeded for channel and recipient");
        }
    }
}
