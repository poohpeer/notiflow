package com.alex.notiflow.api.repository;

import com.alex.notiflow.api.domain.NotificationEntity;
import com.alex.notiflow.contracts.NotificationChannel;
import com.alex.notiflow.contracts.NotificationStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    Optional<NotificationEntity> findByIdempotencyKey(String idempotencyKey);

    Page<NotificationEntity> findByChannelAndStatus(NotificationChannel channel, NotificationStatus status, Pageable pageable);

    Page<NotificationEntity> findByChannel(NotificationChannel channel, Pageable pageable);

    Page<NotificationEntity> findByStatus(NotificationStatus status, Pageable pageable);

    @Modifying
    @Query("""
            update NotificationEntity notification
            set notification.status = :nextStatus,
                notification.updatedAt = :updatedAt
            where notification.id = :id
              and notification.status = :expectedStatus
            """)
    int updateStatusIfCurrentStatus(
            @Param("id") UUID id,
            @Param("expectedStatus") NotificationStatus expectedStatus,
            @Param("nextStatus") NotificationStatus nextStatus,
            @Param("updatedAt") Instant updatedAt
    );
}
