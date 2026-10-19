package com.energymonitor.recommendation.adapter.out.persistence;

import com.energymonitor.recommendation.adapter.out.persistence.entity.RecommendationEntity;
import com.energymonitor.recommendation.adapter.out.persistence.repository.RecommendationRepository;
import com.energymonitor.recommendation.api.RecommendationStatus;
import com.energymonitor.recommendation.api.RecommendationType;
import com.energymonitor.recommendation.application.port.out.RecommendationPersistencePort;
import com.energymonitor.recommendation.domain.model.Recommendation;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persistence adapter for {@link Recommendation}.
 */
@Component
public class RecommendationPersistenceAdapter implements RecommendationPersistencePort {

    private final RecommendationRepository repository;

    public RecommendationPersistenceAdapter(RecommendationRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Recommendation save(Recommendation r) {
        RecommendationEntity entity = repository.findById(r.idRecommendation())
                .map(existing -> {
                    existing.setStatus(r.status());
                    return existing;
                })
                .orElseGet(() -> new RecommendationEntity(r.idRecommendation(), r.homeId(), r.deviceId(),
                        r.type(), r.messageKey(), r.dateTime().truncatedTo(ChronoUnit.SECONDS), r.status()));
        repository.save(entity);
        return r;
    }

    @Override
    public Optional<Recommendation> findActive(String idRecommendation) {
        return repository.findById(idRecommendation)
                .filter(e -> e.getDeletedAt() == null)
                .map(RecommendationPersistenceAdapter::toDomain);
    }

    @Override
    public List<Recommendation> listActiveByHome(String homeId) {
        return repository.findByHomeIdAndDeletedAtIsNullOrderByDateTimeDesc(homeId).stream()
                .map(RecommendationPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public boolean existsSince(String homeId, RecommendationType type, String deviceId, Instant since) {
        return repository.existsSince(homeId, type, deviceId, since);
    }

    @Override
    @Transactional
    public void delete(String idRecommendation) {
        repository.softDelete(idRecommendation, now());
    }

    @Override
    @Transactional
    public int deleteRead(String homeId) {
        return repository.softDeleteByStatus(homeId, RecommendationStatus.READ, now());
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private static Recommendation toDomain(RecommendationEntity e) {
        return new Recommendation(e.getIdRecommendation(), e.getHomeId(), e.getDeviceId(), e.getType(),
                e.getMessageKey(), e.getDateTime(), e.getStatus());
    }
}
