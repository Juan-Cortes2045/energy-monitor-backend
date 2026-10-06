package com.energymonitor.notification.adapter.out.persistence.repository;

import com.energymonitor.notification.adapter.out.persistence.entity.NotificationEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data repository over {@code notification}.
 *
 * <p>No finder takes a message body, so no code path can put one in a query. The bodies live only in
 * the message that left, never in the row that records that it did.
 */
public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {

    /**
     * The pending rows opened before a cutoff, oldest first.
     *
     * <p>Ordered so a sweep that has to stop part way closes the oldest first, which are the ones
     * that have been lying longest.
     *
     * @param openedBefore the cutoff, exclusive
     * @param limit        how many rows to return
     * @return the abandoned rows
     */
    List<NotificationEntity> findByDeliveryStatusAndCreatedAtLessThanOrderByCreatedAtAsc(
            com.energymonitor.notification.domain.model.NotificationStatus status,
            LocalDateTime openedBefore,
            org.springframework.data.domain.Pageable limit);

    /**
     * Closes every pending row older than a cutoff, and only those.
     *
     * <p>The {@code status} predicate is what makes the sweep safe to run while deliveries are still
     * going out: a row that completed a moment ago is no longer pending and is left alone.
     *
     * @param status       the state to close, always pending
     * @param openedBefore the cutoff, exclusive
     * @param reason       recorded on each row it closes
     * @return how many rows were closed
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update NotificationEntity n set n.deliveryStatus = :failed, n.failureReason = :reason"
            + " where n.deliveryStatus = :pending and n.createdAt < :openedBefore" 
            + " and n.deletedAt is null")
    int failAbandoned(@Param("pending") com.energymonitor.notification.domain.model.NotificationStatus pending,
            @Param("openedBefore") LocalDateTime openedBefore,
            @Param("reason") String reason,
            @Param("failed") com.energymonitor.notification.domain.model.NotificationStatus failed);
}