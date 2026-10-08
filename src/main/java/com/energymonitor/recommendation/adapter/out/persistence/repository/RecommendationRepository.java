package com.energymonitor.recommendation.adapter.out.persistence.repository;

import com.energymonitor.recommendation.adapter.out.persistence.entity.RecommendationEntity;
import com.energymonitor.recommendation.api.RecommendationStatus;
import com.energymonitor.recommendation.api.RecommendationType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository over {@code recommendation}.
 */
@Repository
public interface RecommendationRepository extends JpaRepository<RecommendationEntity, String> {

    List<RecommendationEntity> findByHomeIdAndDeletedAtIsNullOrderByDateTimeDesc(String homeId);

    @Query("""
            select count(r) > 0 from RecommendationEntity r
             where r.homeId = :homeId and r.type = :type and r.dateTime >= :since
               and ((:deviceId is null and r.deviceId is null) or r.deviceId = :deviceId)
            """)
    boolean existsSince(@Param("homeId") String homeId, @Param("type") RecommendationType type,
                        @Param("deviceId") String deviceId, @Param("since") Instant since);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RecommendationEntity r set r.deletedAt = :now
             where r.idRecommendation = :id and r.deletedAt is null
            """)
    int softDelete(@Param("id") String idRecommendation, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RecommendationEntity r set r.deletedAt = :now
             where r.homeId = :homeId and r.status = :status and r.deletedAt is null
            """)
    int softDeleteByStatus(@Param("homeId") String homeId, @Param("status") RecommendationStatus status,
                           @Param("now") Instant now);
}
