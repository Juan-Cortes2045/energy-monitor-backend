package com.energymonitor.security.adapter.out.persistence;

import com.energymonitor.security.adapter.out.persistence.entity.PasswordResetTokenEntity;
import com.energymonitor.security.adapter.out.persistence.mapper.PasswordResetTokenMapper;
import com.energymonitor.security.adapter.out.persistence.repository.PasswordResetTokenRepository;
import com.energymonitor.security.application.port.out.PasswordResetTokenPersistencePort;
import com.energymonitor.security.domain.model.PasswordResetToken;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Persistence adapter for {@link PasswordResetToken}.
 *
 * <p>Tokens are one-shot: the domain mutates {@code used} and the adapter updates the row by
 * mapping the full domain state onto the existing one. Because the rehydrated domain carries
 * {@code used}, the stored flag is the only source of truth and can never be overridden by a
 * stale in-memory object.
 */
@Component
public class PasswordResetTokenPersistenceAdapter implements PasswordResetTokenPersistencePort {

    private final PasswordResetTokenRepository repository;
    private final PasswordResetTokenMapper mapper;

    public PasswordResetTokenPersistenceAdapter(PasswordResetTokenRepository repository,
            PasswordResetTokenMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Inserts a token. The token's issue instant is written into {@code created_at}.
     *
     * @param token the domain object
     * @return the same domain object
     */
    public PasswordResetToken save(PasswordResetToken token) {
        repository.save(mapper.toEntity(token));
        return token;
    }

    /**
     * Updates the state (used flag) of a stored token.
     *
     * @param token the domain object holding the new state
     * @return the same domain object
     */
    public PasswordResetToken update(PasswordResetToken token) {
        PasswordResetTokenEntity entity = repository.findById(token.idResetToken())
                .map(existing -> {
                    mapper.applyTo(existing, token);
                    return existing;
                })
                .orElseGet(() -> mapper.toEntity(token));
        // an existing row's created_at must survive; the insert path presets it
        repository.save(entity);
        return token;
    }

    /**
     * Finds the active token by identifier.
     *
     * @param idResetToken the identifier
     * @return the domain object, empty when soft-deleted or missing
     */
    public Optional<PasswordResetToken> findActive(String idResetToken) {
        return repository.findById(idResetToken)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(mapper::toDomain);
    }

    /**
     * Finds the active token by its opaque value.
     *
     * @param resetToken the value
     * @return the domain object, empty when soft-deleted or unknown
     */
    public Optional<PasswordResetToken> findActiveByValue(String resetToken) {
        return repository.findByResetTokenAndDeletedAtIsNull(resetToken)
                .map(mapper::toDomain);
    }

    /**
     * Lists the active tokens of a user, newest first.
     *
     * @param idUser the recipient
     * @return the pending tokens
     */
    public List<PasswordResetToken> listActiveByUser(String idUser) {
        return repository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(idUser).stream()
                .map(mapper::toDomain)
                .toList();
    }
}