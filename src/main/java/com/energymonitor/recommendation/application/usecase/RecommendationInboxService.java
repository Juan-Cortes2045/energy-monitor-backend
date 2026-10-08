package com.energymonitor.recommendation.application.usecase;

import com.energymonitor.recommendation.application.exception.RecommendationNotFoundException;
import com.energymonitor.recommendation.application.port.in.RecommendationInbox;
import com.energymonitor.recommendation.application.port.in.RecommendationView;
import com.energymonitor.recommendation.application.port.out.HomeReaderPort;
import com.energymonitor.recommendation.application.port.out.RecommendationPersistencePort;
import com.energymonitor.recommendation.domain.model.Recommendation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Inbox of recommendations. This service contains no Spring annotations.
 */
public class RecommendationInboxService implements RecommendationInbox {

    private final RecommendationPersistencePort recommendations;
    private final HomeReaderPort homes;

    public RecommendationInboxService(RecommendationPersistencePort recommendations, HomeReaderPort homes) {
        this.recommendations = recommendations;
        this.homes = homes;
    }

    @Override
    public List<RecommendationView> list(String userId, String homeId) {
        requireMember(userId, homeId);
        Map<String, Optional<String>> names = new HashMap<>();
        return recommendations.listActiveByHome(homeId).stream()
                .map(r -> toView(r, r.deviceId() == null ? null
                        : names.computeIfAbsent(r.deviceId(), homes::deviceName).orElse(null)))
                .toList();
    }

    @Override
    public RecommendationView markRead(String userId, String idRecommendation) {
        Recommendation recommendation = owned(userId, idRecommendation);
        if (!recommendation.isRead()) {
            recommendation.markRead();
            recommendations.save(recommendation);
        }
        return toView(recommendation, deviceName(recommendation));
    }

    @Override
    public void delete(String userId, String idRecommendation) {
        Recommendation recommendation = owned(userId, idRecommendation);
        if (!recommendation.isRead()) {
            throw new IllegalStateException("recommendation " + idRecommendation + " is still unread");
        }
        recommendations.delete(idRecommendation);
    }

    @Override
    public int deleteRead(String userId, String homeId) {
        requireMember(userId, homeId);
        return recommendations.deleteRead(homeId);
    }

    private Recommendation owned(String userId, String idRecommendation) {
        return recommendations.findActive(idRecommendation)
                .filter(r -> homes.isMember(userId, r.homeId()))
                .orElseThrow(() -> new RecommendationNotFoundException("no recommendation " + idRecommendation));
    }

    private void requireMember(String userId, String homeId) {
        if (!homes.isMember(userId, homeId)) {
            throw new RecommendationNotFoundException("no recommendations for home " + homeId);
        }
    }

    private String deviceName(Recommendation r) {
        return r.deviceId() == null ? null : homes.deviceName(r.deviceId()).orElse(null);
    }

    private static RecommendationView toView(Recommendation r, String deviceName) {
        return new RecommendationView(r.idRecommendation(), r.homeId(), r.deviceId(), deviceName,
                r.type(), r.messageKey(), r.dateTime(), r.status());
    }
}
