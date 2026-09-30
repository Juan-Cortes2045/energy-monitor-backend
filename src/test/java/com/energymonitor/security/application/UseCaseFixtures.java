package com.energymonitor.security.application;

import com.energymonitor.security.application.port.out.AuditLogPersistencePort;
import com.energymonitor.security.application.port.out.IdentifierGeneratorPort;
import com.energymonitor.security.application.port.out.LoginErrorLogPersistencePort;
import com.energymonitor.security.application.port.out.PasswordHasherPort;
import com.energymonitor.security.application.port.out.PasswordPolicyPersistencePort;
import com.energymonitor.security.application.port.out.PasswordResetTokenPersistencePort;
import com.energymonitor.security.application.port.out.PermissionPersistencePort;
import com.energymonitor.security.application.port.out.PersonPersistencePort;
import com.energymonitor.security.application.port.out.SystemRolePermissionPersistencePort;
import com.energymonitor.security.application.port.out.SystemRolePersistencePort;
import com.energymonitor.security.application.port.out.TokenGeneratorPort;
import com.energymonitor.security.application.port.out.UserPersistencePort;
import com.energymonitor.security.application.port.out.UserSessionPersistencePort;
import com.energymonitor.security.application.port.out.UserSystemRolePersistencePort;
import com.energymonitor.security.domain.model.AuditLog;
import com.energymonitor.security.domain.model.Email;
import com.energymonitor.security.domain.model.LoginErrorLog;
import com.energymonitor.security.domain.model.PasswordHash;
import com.energymonitor.security.domain.model.PasswordPolicy;
import com.energymonitor.security.domain.model.PasswordResetToken;
import com.energymonitor.security.domain.model.Permission;
import com.energymonitor.security.domain.model.Person;
import com.energymonitor.security.domain.model.SystemRole;
import com.energymonitor.security.domain.model.SystemRolePermission;
import com.energymonitor.security.domain.model.User;
import com.energymonitor.security.domain.model.UserSession;
import com.energymonitor.security.domain.model.UserStatus;
import com.energymonitor.security.domain.model.UserSystemRole;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory fakes of the output ports, for application-layer tests only.
 *
 * <p>They replace the {@code @Component} persistence adapters so the use cases can be
 * exercised without a database. The hasher fake is deliberately naive: {@code hash(raw)} is
 * {@code "hash:" + raw}, which is enough for the application to verify that hashes are
 * produced and compared through the port and never break domain rules.
 */
public final class UseCaseFixtures {

    public static final String PASSWORD = "StrongPass1!";
    public static final Instant REGISTRATION = Instant.parse("2026-01-02T03:04:05Z");

    private UseCaseFixtures() {
    }

    /** Seeds a default password policy: min 8, requires upper case, digits and symbols. */
    public static PasswordPolicy defaultPolicy() {
        return new PasswordPolicy("pol0000001", 8, 64, true, true, true, 90);
    }

    public static final class FakeUserPersistencePort implements UserPersistencePort {

        private final Map<String, User> byId = new LinkedHashMap<>();

        @Override
        public User save(User user) {
            byId.put(user.idUser(), user);
            return user;
        }

        @Override
        public Optional<User> findActive(String idUser) {
            return Optional.ofNullable(byId.get(idUser));
        }

        @Override
        public Optional<User> findActiveByEmail(Email email) {
            return byId.values().stream()
                    .filter(user -> user.email().equals(email))
                    .findFirst();
        }

        public User seed(String idUser, String idPerson, String email, UserStatus status) {
            User user = new User(idUser, idPerson, PasswordHash.of("hash:" + PASSWORD),
                    Email.of(email), true, REGISTRATION, status, 0, null);
            byId.put(idUser, user);
            return user;
        }

        public List<User> users() {
            return new ArrayList<>(byId.values());
        }
    }

    public static final class FakePersonPersistencePort implements PersonPersistencePort {

        private final Map<String, Person> byId = new LinkedHashMap<>();

        @Override
        public Person save(Person person) {
            byId.put(person.idPerson(), person);
            return person;
        }

        @Override
        public Optional<Person> findActive(String idPerson) {
            return Optional.ofNullable(byId.get(idPerson));
        }

        public Person seed(String idPerson) {
            Person person = new Person(idPerson, "Ada", "Lovelace", null, null, null);
            byId.put(idPerson, person);
            return person;
        }
    }

    public static final class FakePasswordHasherPort implements PasswordHasherPort {

        private static final String PREFIX = "hash:";

        @Override
        public PasswordHash hash(String rawPassword) {
            return PasswordHash.of(PREFIX + rawPassword);
        }

        @Override
        public boolean matches(String rawPassword, PasswordHash passwordHash) {
            return passwordHash.value().equals(PREFIX + rawPassword);
        }
    }

    public static final class FakeIdentifierGeneratorPort implements IdentifierGeneratorPort {

        private int sequence;

        @Override
        public String generate() {
            sequence++;
            return "id" + String.format("%07d", sequence);
        }
    }

    public static final class FakeTokenGeneratorPort implements TokenGeneratorPort {

        private int sequence;

        @Override
        public String generateToken() {
            sequence++;
            return "token-" + sequence;
        }
    }

    public static final class FakePasswordPolicyPersistencePort implements PasswordPolicyPersistencePort {

        private PasswordPolicy policy = defaultPolicy();

        @Override
        public PasswordPolicy save(PasswordPolicy policy) {
            this.policy = policy;
            return policy;
        }

        @Override
        public Optional<PasswordPolicy> findActive(String idPasswordPolicy) {
            return policy.idPasswordPolicy().equals(idPasswordPolicy) ? Optional.of(policy) : Optional.empty();
        }

        @Override
        public List<PasswordPolicy> findAllActive() {
            return List.of(policy);
        }

        public FakePasswordPolicyPersistencePort configure(PasswordPolicy policy) {
            this.policy = policy;
            return this;
        }
    }

    public static final class FakeAuditLogPersistencePort implements AuditLogPersistencePort {

        private final List<AuditLog> logs = new ArrayList<>();

        @Override
        public AuditLog save(AuditLog log) {
            logs.add(log);
            return log;
        }

        @Override
        public Optional<AuditLog> findActive(String idAuditLog) {
            return logs.stream()
                    .filter(log -> log.idAuditLog().equals(idAuditLog))
                    .findFirst();
        }

        public List<AuditLog> logs() {
            return logs;
        }
    }

    public static final class FakeLoginErrorLogPersistencePort implements LoginErrorLogPersistencePort {

        private final List<LoginErrorLog> errors = new ArrayList<>();

        @Override
        public LoginErrorLog save(LoginErrorLog log) {
            errors.add(log);
            return log;
        }

        @Override
        public Optional<LoginErrorLog> findActive(String idLoginError) {
            return errors.stream()
                    .filter(log -> log.idLoginError().equals(idLoginError))
                    .findFirst();
        }

        @Override
        public List<LoginErrorLog> listNewestFirst() {
            return new ArrayList<>(errors);
        }

        public List<LoginErrorLog> errors() {
            return errors;
        }
    }

    public static final class FakeUserSessionPersistencePort implements UserSessionPersistencePort {

        private final Map<String, UserSession> byId = new LinkedHashMap<>();

        @Override
        public UserSession save(UserSession session) {
            byId.put(session.idUserSession(), session);
            return session;
        }

        @Override
        public Optional<UserSession> findActive(String idUserSession) {
            return Optional.ofNullable(byId.get(idUserSession));
        }

        @Override
        public Optional<UserSession> findActiveByRefreshToken(String refreshToken) {
            return byId.values().stream()
                    .filter(session -> session.refreshToken().equals(refreshToken))
                    .findFirst();
        }

        @Override
        public List<UserSession> listActiveByUser(String idUser) {
            return byId.values().stream()
                    .filter(session -> session.idUser().equals(idUser))
                    .toList();
        }
    }

    public static final class FakePasswordResetTokenPersistencePort
            implements PasswordResetTokenPersistencePort {

        private final Map<String, PasswordResetToken> byValue = new LinkedHashMap<>();

        @Override
        public PasswordResetToken save(PasswordResetToken token) {
            byValue.put(token.resetToken(), token);
            return token;
        }

        @Override
        public PasswordResetToken update(PasswordResetToken token) {
            return save(token);
        }

        @Override
        public Optional<PasswordResetToken> findActive(String idResetToken) {
            return byValue.values().stream()
                    .filter(token -> token.idResetToken().equals(idResetToken))
                    .findFirst();
        }

        @Override
        public Optional<PasswordResetToken> findActiveByValue(String resetToken) {
            return Optional.ofNullable(byValue.get(resetToken));
        }

        @Override
        public List<PasswordResetToken> listActiveByUser(String idUser) {
            return byValue.values().stream()
                    .filter(token -> token.idUser().equals(idUser))
                    .toList();
        }

        public FakePasswordResetTokenPersistencePort store(PasswordResetToken token) {
            byValue.put(token.resetToken(), token);
            return this;
        }
    }

    public static final class FakeSystemRolePersistencePort implements SystemRolePersistencePort {

        private final Map<String, SystemRole> byId = new LinkedHashMap<>();

        @Override
        public SystemRole save(SystemRole role) {
            byId.put(role.idSystemRole(), role);
            return role;
        }

        @Override
        public Optional<SystemRole> findActive(String idSystemRole) {
            return Optional.ofNullable(byId.get(idSystemRole));
        }

        @Override
        public Optional<SystemRole> findActiveByName(String name) {
            return byId.values().stream()
                    .filter(role -> role.name().equals(name))
                    .findFirst();
        }

        @Override
        public List<SystemRole> findAllActive() {
            return new ArrayList<>(byId.values());
        }

        public SystemRole seed(String idSystemRole, String name, boolean enabled) {
            SystemRole role = new SystemRole(idSystemRole, name, null, enabled);
            byId.put(idSystemRole, role);
            return role;
        }
    }

    public static final class FakePermissionPersistencePort implements PermissionPersistencePort {

        private final Map<String, Permission> byId = new LinkedHashMap<>();

        @Override
        public Permission save(Permission permission) {
            byId.put(permission.idPermission(), permission);
            return permission;
        }

        @Override
        public Optional<Permission> findActive(String idPermission) {
            return Optional.ofNullable(byId.get(idPermission));
        }

        @Override
        public Optional<Permission> findActiveByCode(String code) {
            return byId.values().stream()
                    .filter(permission -> permission.code().equals(code))
                    .findFirst();
        }

        @Override
        public List<Permission> findAllActive() {
            return new ArrayList<>(byId.values());
        }

        public Permission seed(String idPermission, String code) {
            Permission permission = new Permission(idPermission, code, code, null);
            byId.put(idPermission, permission);
            return permission;
        }
    }

    public static final class FakeUserSystemRolePersistencePort implements UserSystemRolePersistencePort {

        private final List<UserSystemRole> assignments = new ArrayList<>();

        @Override
        public UserSystemRole save(UserSystemRole assignment) {
            assignments.removeIf(existing ->
                    existing.links(assignment.idUser(), assignment.idSystemRole()));
            assignments.add(assignment);
            return assignment;
        }

        @Override
        public void remove(String idUser, String idSystemRole) {
            assignments.removeIf(existing -> existing.links(idUser, idSystemRole));
        }

        @Override
        public Optional<UserSystemRole> findActive(String idUser, String idSystemRole) {
            return assignments.stream()
                    .filter(existing -> existing.links(idUser, idSystemRole))
                    .findFirst();
        }

        @Override
        public List<UserSystemRole> listActiveByUser(String idUser) {
            return assignments.stream()
                    .filter(assignment -> assignment.idUser().equals(idUser))
                    .toList();
        }

        @Override
        public List<UserSystemRole> listActiveBySystemRole(String idSystemRole) {
            return assignments.stream()
                    .filter(assignment -> assignment.idSystemRole().equals(idSystemRole))
                    .toList();
        }
    }

    public static final class FakeSystemRolePermissionPersistencePort
            implements SystemRolePermissionPersistencePort {

        private final List<SystemRolePermission> grants = new ArrayList<>();

        @Override
        public SystemRolePermission save(SystemRolePermission grant) {
            grants.removeIf(existing -> existing.links(grant.idSystemRole(), grant.idPermission()));
            grants.add(grant);
            return grant;
        }

        @Override
        public void remove(String idSystemRole, String idPermission) {
            grants.removeIf(existing -> existing.links(idSystemRole, idPermission));
        }

        @Override
        public Optional<SystemRolePermission> findActive(String idSystemRole, String idPermission) {
            return grants.stream()
                    .filter(existing -> existing.links(idSystemRole, idPermission))
                    .findFirst();
        }

        @Override
        public List<SystemRolePermission> listActiveBySystemRole(String idSystemRole) {
            return grants.stream()
                    .filter(grant -> grant.idSystemRole().equals(idSystemRole))
                    .toList();
        }

        @Override
        public List<SystemRolePermission> listActiveByPermission(String idPermission) {
            return grants.stream()
                    .filter(grant -> grant.idPermission().equals(idPermission))
                    .toList();
        }
    }
}