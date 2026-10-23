package com.energymonitor.home.adapter.out.persistence.repository;

import com.energymonitor.home.adapter.out.persistence.entity.HomeOutboxEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository over {@code home_outbox}.
 */
@Repository
public interface HomeOutboxRepository extends JpaRepository<HomeOutboxEntity, String> {

    /**
     * The messages still waiting for the broker, oldest first, so the order in which homes
     * changed is the order in which other services hear about it.
     */
    @Query("""
            select m from HomeOutboxEntity m
            where m.publishedAt is null and m.discardedAt is null
            order by m.occurredAt asc, m.idMessage asc
            """)
    List<HomeOutboxEntity> findPending(Pageable page);
}
