package com.energymonitor.recommendation.application.port.out;

import com.energymonitor.recommendation.api.RecommendationType;
import com.energymonitor.recommendation.domain.model.Recommendation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Storage of recommendations. Deletion is soft.
 */
public interface RecommendationPersistencePort {

    Recommendation save(Recommendation recommendation);

    Optional<Recommendation> findActive(String idRecommendation);

    /** Active recommendations of a home, newest first. */
    List<Recommendation> listActiveByHome(String homeId);

    /**
     * Whether a recommendation of this type (and device, {@code null} = whole home) was generated
     * since an instant, deleted ones included: deleting a suggestion must not bring it straight back.
     */
    boolean existsSince(String homeId, RecommendationType type, String deviceId, Instant since);

    void delete(String idRecommendation);

    /** @return how many READ recommendations of the home were deleted */
    int deleteRead(String homeId);
}
