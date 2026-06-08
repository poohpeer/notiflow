package com.alex.notiflow.api.repository;

import com.alex.notiflow.api.domain.OutboxEventEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
    List<OutboxEventEntity> findByPublishedFalseOrderByCreatedAtAsc(Pageable pageable);
}
