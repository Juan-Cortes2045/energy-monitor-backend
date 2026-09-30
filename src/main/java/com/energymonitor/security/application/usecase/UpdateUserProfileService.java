package com.energymonitor.security.application.usecase;

import com.energymonitor.security.application.command.UpdateUserProfileCommand;
import com.energymonitor.security.application.exception.EmailAlreadyRegisteredException;
import com.energymonitor.security.application.exception.UserNotFoundException;
import com.energymonitor.security.application.port.in.UpdateUserProfile;
import com.energymonitor.security.application.port.out.AuditLogPersistencePort;
import com.energymonitor.security.application.port.out.IdentifierGeneratorPort;
import com.energymonitor.security.application.port.out.PersonPersistencePort;
import com.energymonitor.security.application.port.out.UserPersistencePort;
import com.energymonitor.security.domain.model.AuditAction;
import com.energymonitor.security.domain.model.AuditLog;
import com.energymonitor.security.domain.model.Email;
import com.energymonitor.security.domain.model.Person;
import com.energymonitor.security.domain.model.User;
import java.time.Clock;
import java.time.Instant;

/**
 * Edits the personal and contact data of a user and, optionally, the account address.
 */
public class UpdateUserProfileService implements UpdateUserProfile {

    private final UserPersistencePort userPort;
    private final PersonPersistencePort personPort;
    private final AuditLogPersistencePort auditLogPort;
    private final IdentifierGeneratorPort identifiers;
    private final Clock clock;

    public UpdateUserProfileService(UserPersistencePort userPort, PersonPersistencePort personPort,
                                    AuditLogPersistencePort auditLogPort,
                                    IdentifierGeneratorPort identifiers, Clock clock) {
        this.userPort = userPort;
        this.personPort = personPort;
        this.auditLogPort = auditLogPort;
        this.identifiers = identifiers;
        this.clock = clock;
    }

    @Override
    public Person update(UpdateUserProfileCommand command) {
        Instant now = clock.instant();
        User user = userPort.findActive(command.idUser())
                .orElseThrow(() -> new UserNotFoundException("no active user " + command.idUser()));
        Person person = personPort.findActive(user.idPerson())
                .orElseThrow(() -> new UserNotFoundException("no active person " + user.idPerson()));

        if (command.name() != null && command.lastName() != null) {
            person.rename(command.name(), command.lastName());
        }
        if (command.cellphone() != null || command.address() != null || command.profileImage() != null) {
            person.updateContactData(command.cellphone(), command.address(), command.profileImage());
        }
        personPort.save(person);

        if (command.newEmail() != null) {
            Email newEmail = Email.of(command.newEmail());
            userPort.findActiveByEmail(newEmail).ifPresent(existing -> {
                if (!existing.equals(user)) {
                    throw new EmailAlreadyRegisteredException("an account already exists for " + newEmail);
                }
            });
            user.changeEmail(newEmail);
            userPort.save(user);
        }

        auditLogPort.save(new AuditLog(identifiers.generate(), user.idUser(), AuditAction.UPDATE,
                null, command.ipAddress(), null, now));
        return person;
    }
}