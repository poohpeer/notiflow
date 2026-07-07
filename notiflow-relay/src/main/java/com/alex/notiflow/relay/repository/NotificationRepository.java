package com.alex.notiflow.relay.repository;

import com.alex.notiflow.contracts.NotificationStatus;
import com.alex.notiflow.relay.domain.NotificationEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

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
