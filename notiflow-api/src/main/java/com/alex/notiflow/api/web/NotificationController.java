package com.alex.notiflow.api.web;

import com.alex.notiflow.api.service.NotificationService;
import com.alex.notiflow.contracts.NotificationAcceptedResponse;
import com.alex.notiflow.contracts.NotificationChannel;
import com.alex.notiflow.contracts.NotificationRequest;
import com.alex.notiflow.contracts.NotificationStatus;
import com.alex.notiflow.contracts.NotificationStatusResponse;

import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public NotificationAcceptedResponse create(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody NotificationRequest request) {
        return notificationService.create(idempotencyKey, request);
    }

    @GetMapping("/{id}")
    public NotificationStatusResponse get(@PathVariable UUID id) {
        return notificationService.get(id);
    }

    @GetMapping
    public Page<NotificationStatusResponse> list(NotificationChannel channel, NotificationStatus status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return notificationService.list(channel, status, pageable);
    }
}
