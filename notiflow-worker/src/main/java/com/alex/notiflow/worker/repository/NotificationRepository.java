package com.alex.notiflow.worker.repository;

import com.alex.notiflow.worker.domain.NotificationEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
}
