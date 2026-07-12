package com.alex.notiflow.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.data.redis.RedisSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Keeps the login session in Redis instead of Tomcat's heap, so it survives an
 * api restart and is shared across replicas.
 *
 * <p>Enabled explicitly rather than through {@code spring.session.store-type}:
 * that property is not honoured by the Boot autoconfiguration here, and the
 * silent fallback is an in-memory session (verified — no session keys in Redis).
 */
@Configuration
@EnableRedisHttpSession(redisNamespace = "notiflow:session")
public class SessionConfig {

    @Bean
    SessionRepositoryCustomizer<RedisSessionRepository> sessionTimeoutCustomizer(SecurityProperties properties) {
        return repository -> repository.setDefaultMaxInactiveInterval(properties.sessionTimeout());
    }
}
