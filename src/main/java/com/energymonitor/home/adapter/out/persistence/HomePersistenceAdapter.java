package com.energymonitor.home.adapter.out.persistence;

import com.energymonitor.home.adapter.out.persistence.entity.HomeEntity;
import com.energymonitor.home.adapter.out.persistence.mapper.HomeMapper;
import com.energymonitor.home.adapter.out.persistence.repository.HomeRepository;
import com.energymonitor.home.application.exception.HomeAccessCodeCollisionException;
import com.energymonitor.home.application.port.out.HomePersistencePort;
import com.energymonitor.home.domain.model.Home;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Persistence adapter for {@link Home}.
 */
@Component
public class HomePersistenceAdapter implements HomePersistencePort {

    private final HomeRepository repository;
    private final HomeMapper mapper;

    public HomePersistenceAdapter(HomeRepository repository, HomeMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Home save(Home home) {
        try {
            HomeEntity entity = repository.findById(home.idHome())
                    .map(existing -> {
                        mapper.applyTo(existing, home);
                        return existing;
                    })
                    .orElseGet(() -> mapper.toEntity(home));
            // Flushed here on purpose. The INSERT for a new row is otherwise deferred to
            // commit, which happens outside this method and outside this catch, so a duplicate
            // access code would escape as a raw Spring failure instead of the domain signal the
            // caller is told to expect.
            repository.saveAndFlush(entity);
            return home;
        } catch (DataIntegrityViolationException e) {
            // Translate unique constraint violation (access_code) to a domain-level signal
            throw new HomeAccessCodeCollisionException("access code already exists", e);
        }
    }

    @Override
    public Optional<Home> findActive(String idHome) {
        return repository.findById(idHome)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Home> findActiveByAccessCode(String accessCode) {
        return repository.findByAccessCodeAndDeletedAtIsNull(accessCode)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsActiveByAccessCode(String accessCode) {
        return repository.findByAccessCodeAndDeletedAtIsNull(accessCode).isPresent();
    }

    @Override
    public List<Home> findActiveByIds(Collection<String> ids) {
        return repository.findByIdHomeInAndDeletedAtIsNull(ids).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
