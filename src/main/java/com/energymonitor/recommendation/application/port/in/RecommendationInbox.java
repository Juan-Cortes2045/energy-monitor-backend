package com.energymonitor.recommendation.application.port.in;

import java.util.List;

/**
 * What a member can do with the recommendations of a home. Every operation checks membership:
 * a non-member gets {@code RecommendationNotFoundException}.
 */
public interface RecommendationInbox {

    List<RecommendationView> list(String userId, String homeId);

    RecommendationView markRead(String userId, String idRecommendation);

    /** Deletes a read recommendation; an unread one is an {@code IllegalStateException}. */
    void delete(String userId, String idRecommendation);

    /** @return how many read recommendations were deleted */
    int deleteRead(String userId, String homeId);
}
