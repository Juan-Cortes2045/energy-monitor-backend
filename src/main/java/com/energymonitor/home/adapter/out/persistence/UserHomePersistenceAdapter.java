package com.energymonitor.home.adapter.out.persistence;

import com.energymonitor.home.adapter.out.persistence.entity.UserHomeEntity;
import com.energymonitor.home.adapter.out.persistence.entity.UserHomeId;
import com.energymonitor.home.adapter.out.persistence.mapper.UserHomeMapper;
import com.energymonitor.home.adapter.out.persistence.repository.UserHomeRepository;
import com.energymonitor.home.adapter.out.persistence.support.Instants;
import com.energymonitor.home.application.port.out.UserHomePersistencePort;
import com.energymonitor.home.domain.model.Role;
import com.energymonitor.home.domain.model.UserHome;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Persistence adapter for {@link UserHome}.
 *
 * <p>Implements the reactivation strategy: when a row with the same composite key exists
 * but is soft-deleted, the adapter clears {@code deleted_at} and revives it.
 */
@Component
public class UserHomePersistenceAdapter implements UserHomePersistencePort {

    private final UserHomeRepository repository;
    private final UserHomeMapper mapper;

    public UserHomePersistenceAdapter(UserHomeRepository repository, UserHomeMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public UserHome save(UserHome membership) {
        UserHomeId id = new UserHomeId(membership.userId(), membership.homeId());
        UserHomeEntity entity = repository.findById(id)
                .map(existing -> {
                    if (existing.getDeletedAt() != null) {
                        existing.setDeletedAt(null);
                    }
                    mapper.applyTo(existing, membership);
                    return existing;
                })
                .orElseGet(() -> mapper.toEntity(membership));
        repository.save(entity);
        return membership;
    }

    @Override
    public void remove(String userId, String homeId) {
        repository.findById(new UserHomeId(userId, homeId))
                .ifPresent(entity -> {
                    entity.setDeletedAt(Instants.now());
                    repository.save(entity);
                });
    }

    @Override
    public Optional<UserHome> findActive(String userId, String homeId) {
        return repository.findById(new UserHomeId(userId, homeId))
                .filter(entity -> entity.getDeletedAt() == null)
                .map(mapper::toDomain);
    }

    @Override
    public List<UserHome> listActiveByUser(String userId) {
        return repository.findByIdUserIdAndDeletedAtIsNull(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<UserHome> listActiveByHomeId(String homeId) {
        return repository.findByIdHomeIdAndDeletedAtIsNull(homeId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countActiveOwnersByHomeId(String homeId) {
        return repository.countByIdHomeIdAndRoleAndDeletedAtIsNull(homeId, Role.OWNER);
    }
}
