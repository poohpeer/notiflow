package com.alex.notiflow.relay.repository;

import com.alex.notiflow.relay.domain.OutboxEventEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    /**
     * Claims a batch of unpublished events for this relay instance.
     * FOR UPDATE SKIP LOCKED lets multiple relay replicas pull disjoint batches
     * concurrently: rows locked by another instance are skipped, not waited on.
     * The lock is held until the surrounding transaction commits.
     */
    @Query(value = """
            select * from outbox_events
            where published = false
            order by created_at asc
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<OutboxEventEntity> lockPendingBatch(@Param("limit") int limit);
}
