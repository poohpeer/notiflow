package com.alex.notiflow.api.config;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Session-based auth for the SPA. The session id lives in an HttpOnly cookie and
 * the session itself in Redis (spring-session-data-redis), so it survives an api
 * restart and works across replicas.
 *
 * <p>Left open: the actuator endpoints, because Prometheus scrapes
 * {@code /actuator/prometheus} without credentials, and the login endpoint.
 */
@Configuration
@EnableWebSecurity
@org.springframework.boot.context.properties.EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        // Cookie-based CSRF: the SPA reads the non-HttpOnly XSRF-TOKEN cookie and
        // echoes it back in the X-XSRF-TOKEN header on every mutating request.
        var csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        var csrfTokenHandler = new CsrfTokenRequestAttributeHandler();

        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(csrfTokenHandler)
                        // Prometheus scrapes over plain GET; nothing to protect there.
                        .ignoringRequestMatchers("/actuator/**"))
                .addFilterAfter(new CsrfCookieFilter(), org.springframework.security.web.csrf.CsrfFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .anyRequest().authenticated())
                // An SPA must get a 401 it can react to, not a 302 to a login page.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedEntryPoint(objectMapper)))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .build();
    }

    /**
     * CsrfToken is looked up lazily, so without touching it the XSRF-TOKEN cookie is
     * never written and the SPA has no token to send on its first mutating request.
     */
    static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            var csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            chain.doFilter(request, response);
        }
    }

    private AuthenticationEntryPoint unauthorizedEntryPoint(ObjectMapper objectMapper) {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "timestamp", Instant.now().toString(),
                    "status", HttpStatus.UNAUTHORIZED.value(),
                    "error", HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                    "message", "Authentication required"));
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(SecurityProperties properties) {
        return new InMemoryUserDetailsManager(User.withUsername(properties.username())
                .password(properties.passwordHash())
                .roles("USER")
                .build());
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
