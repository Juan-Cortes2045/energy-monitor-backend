package com.energymonitor.security.infrastructure;

import com.energymonitor.security.application.port.in.AssignRole;
import com.energymonitor.security.application.port.in.AuthenticateUser;
import com.energymonitor.security.application.port.in.ChangePassword;
import com.energymonitor.security.application.port.in.CheckPermission;
import com.energymonitor.security.application.port.in.CreatePasswordResetToken;
import com.energymonitor.security.application.port.in.CreateUserSession;
import com.energymonitor.security.application.port.in.FindUser;
import com.energymonitor.security.application.port.in.ManageUserStatus;
import com.energymonitor.security.application.port.in.RegisterUser;
import com.energymonitor.security.application.port.in.RefreshSession;
import com.energymonitor.security.application.port.in.ResetPassword;
import com.energymonitor.security.application.port.in.RevokeRole;
import com.energymonitor.security.application.port.in.LogoutUserSession;
import com.energymonitor.security.application.port.in.UpdateUserProfile;
import com.energymonitor.security.application.port.out.AuditLogPersistencePort;
import com.energymonitor.security.application.port.out.IdentifierGeneratorPort;
import com.energymonitor.security.application.port.out.LoginErrorLogPersistencePort;
import com.energymonitor.security.application.port.out.PasswordHasherPort;
import com.energymonitor.security.application.port.out.RefreshTokenHasherPort;
import com.energymonitor.security.application.port.out.RefreshTokenPersistencePort;
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
import com.energymonitor.security.application.usecase.AssignRoleService;
import com.energymonitor.security.application.usecase.AuthenticateUserService;
import com.energymonitor.security.application.usecase.ChangePasswordService;
import com.energymonitor.security.application.usecase.CheckPermissionService;
import com.energymonitor.security.application.usecase.CreatePasswordResetTokenService;
import com.energymonitor.security.application.usecase.CreateUserSessionService;
import com.energymonitor.security.application.usecase.FindUserService;
import com.energymonitor.security.application.usecase.ManageUserStatusService;
import com.energymonitor.security.application.usecase.RegisterUserService;
import com.energymonitor.security.application.usecase.RefreshSessionService;
import com.energymonitor.security.application.usecase.ResetPasswordService;
import com.energymonitor.security.application.usecase.RevokeRoleService;
import com.energymonitor.security.application.usecase.LogoutUserSessionService;
import com.energymonitor.security.application.usecase.UpdateUserProfileService;
import java.time.Clock;
import org.aopalliance.aop.Advice;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Wires the Security application layer into the Spring context.
 *
 * <p>This is the only place that knows both halves of the architecture: it instantiates the
 * use cases, which stay plain Java classes with no annotations of their own, from the output
 * ports, which the persistence and security adapters implement as Spring beans.
 *
 * <h2>Transaction boundaries</h2>
 *
 * <p>The boundary is created here, as an explicit proxy around the use case, rather than with
 * {@code @Transactional} on the use case class. Two reasons, and the first one is a trap:
 * annotating the {@code @Bean} factory methods below would be silently inert, because
 * Spring's auto-proxy creator decides whether to wrap a bean by inspecting the <em>target
 * class</em> for {@code @Transactional} and never looks at the factory method that produced
 * it. The result would look configured and commit nothing. The second reason is the one that
 * matters architecturally: the use cases must not import Spring, so the annotation cannot go
 * on them at all.
 *
 * <p>Building the proxy here makes the boundary verifiable instead of assumed.
 * {@code SecurityTransactionBoundaryTest} asserts that each of these beans really is advised
 * by a {@link TransactionInterceptor} and that the single-write and read-only flows really are
 * not, so a later refactor cannot quietly move or drop one.
 *
 * <p>Multi-write flows get the boundary because a partial commit would leave inconsistent
 * state:
 *
 * <ul>
 *   <li>{@link RegisterUserService} - person, account and audit row.</li>
 *   <li>{@link UpdateUserProfileService} - person, optional account, audit row.</li>
 *   <li>{@link AuthenticateUserService} - the account's failure counter together with its
 *       audit or typed-error row. Partial failure here would either erase a failed-attempt
 *       record or reset the counter without a matching login.</li>
 *   <li>{@link ChangePasswordService} - new hash and the audit trail of the change.</li>
 *   <li>{@link ResetPasswordService} - new hash, token consumption and audit row. Partial
 *       failure would leave the password changed but the token still reusable.</li>
 *   <li>{@link LogoutUserSessionService} - session closure and its logout audit row.</li>
 *   <li>{@link AssignRoleService} / {@link RevokeRoleService} - assignment and audit row.</li>
 *   <li>{@link ManageUserStatusService} - account state and audit row.</li>
 * </ul>
 *
 * <p>{@link CreateUserSessionService}, {@link CreatePasswordResetTokenService},
 * {@link FindUserService} and {@link CheckPermissionService} are read-only or write a single
 * row, so a transaction would add nothing but an extra connection and are deliberately left
 * without a boundary.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityApplicationWiring {

    /**
     * The clock every use case receives, so time is a constructor dependency rather than a
     * static call and tests can pin it.
     *
     * @return a UTC clock
     */
    @Bean
    public Clock securityClock() {
        return Clock.systemUTC();
    }

    /**
     * Wraps a use case in a proxy that runs it inside a transaction.
     *
     * <p>The proxy implements only the input port, so callers depend on the interface and the
     * application layer stays unaware that a proxy sits between it and the Spring context.
     * Rollback is the default: any {@link RuntimeException} the use case raises, including
     * every typed application failure, leaves the database untouched.
     *
     * @param <T>          the input port type
     * @param transactions  the transaction manager
     * @param target        the use case to protect
     * @param inputPort     the interface callers inject
     * @return the transactional proxy
     */
    private <T> T transactional(PlatformTransactionManager transactions, T target,
                                Class<T> inputPort) {
        TransactionAttributeSource everyMethod = new TransactionAttributeSource() {
            @Override
            public org.springframework.transaction.interceptor.TransactionAttribute
            getTransactionAttribute(java.lang.reflect.Method method, Class<?> targetClass) {
                return new DefaultTransactionAttribute();
            }
        };
        Advice transactionAdvice = new TransactionInterceptor(transactions, everyMethod);
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.setInterfaces(inputPort);
        proxyFactory.addAdvice(transactionAdvice);
        return inputPort.cast(proxyFactory.getProxy());
    }

    @Bean
    public RegisterUser registerUser(UserPersistencePort users, PersonPersistencePort persons,
                                     PasswordPolicyPersistencePort policies,
                                     PasswordHasherPort hasher, IdentifierGeneratorPort identifiers,
                                     AuditLogPersistencePort audits, Clock clock,
                                     PlatformTransactionManager transactions) {
        return transactional(transactions,
                new RegisterUserService(users, persons, policies, hasher, identifiers, audits,
                        clock),
                RegisterUser.class);
    }

    @Bean
    public AuthenticateUser authenticateUser(UserPersistencePort users, PasswordHasherPort hasher,
                                             IdentifierGeneratorPort identifiers,
                                             AuditLogPersistencePort audits,
                                             LoginErrorLogPersistencePort loginErrorLog, Clock clock,
                                             PlatformTransactionManager transactions) {
        return transactional(transactions,
                new AuthenticateUserService(users, hasher, identifiers, audits, loginErrorLog,
                        clock),
                AuthenticateUser.class);
    }

    @Bean
    public UpdateUserProfile updateUserProfile(UserPersistencePort users,
                                               PersonPersistencePort persons,
                                               AuditLogPersistencePort audits,
                                               IdentifierGeneratorPort identifiers, Clock clock,
                                               PlatformTransactionManager transactions) {
        return transactional(transactions,
                new UpdateUserProfileService(users, persons, audits, identifiers, clock),
                UpdateUserProfile.class);
    }

    @Bean
    public ManageUserStatus manageUserStatus(UserPersistencePort users,
                                             AuditLogPersistencePort audits,
                                             IdentifierGeneratorPort identifiers, Clock clock,
                                             PlatformTransactionManager transactions) {
        return transactional(transactions,
                new ManageUserStatusService(users, audits, identifiers, clock),
                ManageUserStatus.class);
    }

    @Bean
    public ChangePassword changePassword(UserPersistencePort users, PasswordHasherPort hasher,
                                         PasswordPolicyPersistencePort policies,
                                         AuditLogPersistencePort audits,
                                         IdentifierGeneratorPort identifiers, Clock clock,
                                         PlatformTransactionManager transactions) {
        return transactional(transactions,
                new ChangePasswordService(users, hasher, policies, audits, identifiers, clock),
                ChangePassword.class);
    }

    @Bean
    public ResetPassword resetPassword(UserPersistencePort users,
                                       PasswordResetTokenPersistencePort tokens,
                                       PasswordHasherPort hasher,
                                       PasswordPolicyPersistencePort policies,
                                       AuditLogPersistencePort audits,
                                       IdentifierGeneratorPort identifiers, Clock clock,
                                       PlatformTransactionManager transactions) {
        return transactional(transactions,
                new ResetPasswordService(users, tokens, hasher, policies, audits, identifiers,
                        clock),
                ResetPassword.class);
    }

    /**
     * Writes a single row, so a transaction would add nothing but a second connection.
     *
     * @param users          account storage
     * @param tokens         reset token storage
     * @param tokenGenerator token generation port
     * @param identifiers    identifier port
     * @param clock          time source
     * @return the use case
     */
    @Bean
    public CreatePasswordResetToken createPasswordResetToken(
            UserPersistencePort users, PasswordResetTokenPersistencePort tokens,
            TokenGeneratorPort tokenGenerator, IdentifierGeneratorPort identifiers, Clock clock) {
        return new CreatePasswordResetTokenService(users, tokens, tokenGenerator, identifiers, clock);
    }

    /**
     * Writes a single session row, so no transaction boundary is needed.
     *
     * @param users          account storage
     * @param sessions       session storage
     * @param tokenGenerator token generation port
     * @param identifiers    identifier port
     * @param clock          time source
     * @return the use case
     */
    @Bean
    public CreateUserSession createUserSession(UserPersistencePort users,
                                               UserSessionPersistencePort sessions,
                                               IdentifierGeneratorPort identifiers, Clock clock) {
        return new CreateUserSessionService(users, sessions, identifiers, clock);
    }

    /**
     * Rotates a refresh token. Transactional because a rotation writes the retired generation,
     * its replacement and the session slide together, and a partial application would either
     * retire a token without handing out its successor or leave two active generations in one
     * family.
     *
     * @param tokens   refresh token storage
     * @param sessions session storage
     * @param users    account storage, the source of the identity the new access token is
     *                minted from
     * @param hasher   digest of the presented and issued secrets
     * @param secrets  secure secret generation
     * @param audits   audit trail port
     * @param identifiers identifier port
     * @param clock    time source
     * @return the use case behind a transaction proxy
     */
    @Bean
    public RefreshSession refreshSession(RefreshTokenPersistencePort tokens,
                                         UserSessionPersistencePort sessions,
                                         UserPersistencePort users,
                                         RefreshTokenHasherPort hasher, TokenGeneratorPort secrets,
                                         AuditLogPersistencePort audits,
                                         IdentifierGeneratorPort identifiers, Clock clock,
                                         PlatformTransactionManager transactions) {
        return transactional(transactions,
                new RefreshSessionService(tokens, sessions, users, hasher, secrets, audits,
                        identifiers, clock),
                RefreshSession.class);
    }

    @Bean
    public LogoutUserSession logoutUserSession(UserSessionPersistencePort sessions,
                                               AuditLogPersistencePort audits,
                                               IdentifierGeneratorPort identifiers, Clock clock,
                                               PlatformTransactionManager transactions) {
        return transactional(transactions,
                new LogoutUserSessionService(sessions, audits, identifiers, clock),
                LogoutUserSession.class);
    }

    @Bean
    public AssignRole assignRole(UserPersistencePort users, SystemRolePersistencePort roles,
                                 UserSystemRolePersistencePort assignments,
                                 AuditLogPersistencePort audits, IdentifierGeneratorPort identifiers,
                                 Clock clock, PlatformTransactionManager transactions) {
        return transactional(transactions,
                new AssignRoleService(users, roles, assignments, audits, identifiers, clock),
                AssignRole.class);
    }

    @Bean
    public RevokeRole revokeRole(UserSystemRolePersistencePort assignments,
                                 AuditLogPersistencePort audits,
                                 IdentifierGeneratorPort identifiers, Clock clock,
                                 PlatformTransactionManager transactions) {
        return transactional(transactions,
                new RevokeRoleService(assignments, audits, identifiers, clock),
                RevokeRole.class);
    }

    @Bean
    public FindUser findUser(UserPersistencePort users) {
        return new FindUserService(users);
    }

    @Bean
    public CheckPermission checkPermission(UserPersistencePort users,
                                           PermissionPersistencePort permissions,
                                           SystemRolePersistencePort roles,
                                           UserSystemRolePersistencePort assignments,
                                           SystemRolePermissionPersistencePort grants) {
        return new CheckPermissionService(users, permissions, roles, assignments, grants);
    }
}
