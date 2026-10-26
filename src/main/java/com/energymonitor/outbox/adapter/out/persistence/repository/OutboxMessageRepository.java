package com.energymonitor.outbox.adapter.out.persistence.repository;

import com.energymonitor.outbox.adapter.out.persistence.entity.OutboxMessageEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository over {@code backend_outbox}.
 */
@Repository
public interface OutboxMessageRepository extends JpaRepository<OutboxMessageEntity, String> {

    /**
     * The messages still waiting for the broker, oldest first, so the order in which alerts and
     * recommendations were stored is the order in which other services hear about them.
     */
    @Query("""
            select m from OutboxMessageEntity m
            where m.publishedAt is null and m.discardedAt is null
            order by m.occurredAt asc, m.idMessage asc
            """)
    List<OutboxMessageEntity> findPending(Pageable page);
}
