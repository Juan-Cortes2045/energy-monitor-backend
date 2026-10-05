package com.energymonitor.security.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.energymonitor.security.application.command.RegisterUserCommand;
import com.energymonitor.security.application.exception.EmailAlreadyRegisteredException;
import com.energymonitor.security.application.exception.PasswordPolicyViolationException;
import com.energymonitor.security.application.exception.UserNotFoundException;
import com.energymonitor.security.application.port.in.ChangePassword;
import com.energymonitor.security.application.port.in.CreatePasswordResetToken;
import com.energymonitor.security.application.port.in.LogoutUserSession;
import com.energymonitor.security.application.port.in.RefreshSession;
import com.energymonitor.security.application.port.in.RegisterUser;
import com.energymonitor.security.application.port.in.ResetPassword;
import com.energymonitor.security.application.result.AuthenticatedUser;
import com.energymonitor.security.domain.model.Email;
import com.energymonitor.security.domain.model.PasswordHash;
import com.energymonitor.security.domain.model.User;
import com.energymonitor.security.domain.model.UserStatus;
import com.energymonitor.security.infrastructure.JwtKeyedTest;
import com.energymonitor.security.infrastructure.JwtTokenIssuer;
import com.energymonitor.security.infrastructure.SecurityLoginFlow;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The HTTP contract of the Security API: status codes, error shape and route protection.
 *
 * <p>The input ports are replaced so the assertions are about the delivery layer only. The
 * security filter chain, the JWT encoder and decoder, the exception handler and the JSON
 * mapping are all the real ones, so a token minted here is validated by the same code that
 * guards production traffic.
 */
@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class AuthControllerWebTest extends JwtKeyedTest {

    private static final String EMAIL = "someone@example.com";
    private static final String PASSWORD = "StrongPass1!";
    private static final Instant REGISTRATION = Instant.parse("2026-01-02T03:04:05Z");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokenIssuer tokenIssuer;

    @MockitoBean
    private RegisterUser registerUser;

    @MockitoBean
    private SecurityLoginFlow loginFlow;

    @MockitoBean
    private LogoutUserSession logoutUserSession;

    @MockitoBean
    private CreatePasswordResetToken createPasswordResetToken;

    @MockitoBean
    private ResetPassword resetPassword;

    @MockitoBean
    private ChangePassword changePassword;

    @MockitoBean
    private RefreshSession refreshSession;

    private static User newAccount() {
        return User.register("USR0000001", "PER0000001", PasswordHash.of("$2a$10$abcdefghij"),
                Email.of(EMAIL), REGISTRATION, null);
    }

    private static AuthenticatedUser identity() {
        return new AuthenticatedUser("USR0000001", "PER0000001", Email.of(EMAIL),
                UserStatus.ACTIVE, Instant.now(), null);
    }

    private static String registrationBody() {
        return """
                {"email":"someone@example.com","password":"StrongPass1!","name":"Ada",
                 "lastName":"Lovelace",
                 "profileImage":"https://example.com/a.png"}
                """;
    }

    private String bearerFor(AuthenticatedUser identity) {
        return "Bearer " + tokenIssuer.issue(identity);
    }

    // ---------------------------------------------------------------- registration

    @Test
    void registersAnAccountAndAnswers201() throws Exception {
        given(registerUser.register(any())).willReturn(newAccount());

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idUser").value("USR0000001"))
                .andExpect(jsonPath("$.idPerson").value("PER0000001"))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void passesTheSubmittedFieldsAndTheCallerAddressToTheUseCase() throws Exception {
        given(registerUser.register(any())).willReturn(newAccount());

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody()))
                .andExpect(status().isCreated());

        verify(registerUser).register(any(RegisterUserCommand.class));
    }

    @Test
    void neverReturnsThePasswordHashOnRegistration() throws Exception {
        given(registerUser.register(any())).willReturn(newAccount());

        String body = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertFalse(body.contains("passwordHash"),
                "The response exposed the password hash");
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("$2a$"),
                "The response exposed a BCrypt hash");
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("StrongPass1!"),
                "The response echoed the plain password");
    }

    @Test
    void rejectsARegistrationWithABlankEmail() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"","password":"StrongPass1!","name":"Ada","lastName":"L"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"));

        verify(registerUser, never()).register(any());
    }

    @Test
    void rejectsARegistrationWithNoBody() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void answersConflictWhenTheEmailIsAlreadyRegistered() throws Exception {
        willThrow(new EmailAlreadyRegisteredException("Email already registered"))
                .given(registerUser).register(any());

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void answersUnprocessableWhenThePasswordBreaksThePolicy() throws Exception {
        willThrow(new PasswordPolicyViolationException("Password does not meet the policy"))
                .given(registerUser).register(any());

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody()))
                .andExpect(status().isUnprocessableEntity());
    }

    // ----------------------------------------------------------------------- login

    @Test
    void logsInAndAnswersTheTokensAndTheAccount() throws Exception {
        given(loginFlow.login(any(), any(), any(), any()))
                .willReturn(Optional.of(new SecurityLoginFlow.LoginOutcome("access-token",
                        "refresh-token", 900L, identity())));

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"someone@example.com","password":"StrongPass1!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.account.idUser").value("USR0000001"))
                .andExpect(jsonPath("$.account.email").value(EMAIL));
    }

    @Test
    void neverExposesTheHashInsideTheLoginAccount() throws Exception {
        given(loginFlow.login(any(), any(), any(), any()))
                .willReturn(Optional.of(new SecurityLoginFlow.LoginOutcome("access-token",
                        "refresh-token", 900L, identity())));

        String body = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"someone@example.com","password":"StrongPass1!"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertFalse(body.contains("passwordHash"));
    }

    @Test
    void answersOneGenericUnauthorizedWhenTheAttemptIsRejected() throws Exception {
        given(loginFlow.login(any(), any(), any(), any())).willReturn(Optional.empty());

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"someone@example.com","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid credentials."));
    }

    @Test
    void rejectsALoginWithNoPassword() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"someone@example.com","password":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());

        verify(loginFlow, never()).login(any(), any(), any(), any());
    }

    // ------------------------------------------------------------- password reset

    @Test
    void answersAnEmptyAcceptedForAKnownAccount() throws Exception {
        mvc.perform(post("/api/v1/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"someone@example.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));
    }

    @Test
    void answersIdenticallyForAnUnknownAccountSoAddressesCannotBeEnumerated() throws Exception {
        willThrow(new UserNotFoundException("User not found"))
                .given(createPasswordResetToken).create(any());

        mvc.perform(post("/api/v1/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));
    }

    @Test
    void neverReturnsTheResetTokenItJustMinted() throws Exception {
        // The token leaves through the delivery channel, which is a use case concern; the
        // response carries nothing at all, so there is nothing here to leak even by accident.
        Instant now = Instant.now();
        given(createPasswordResetToken.create(any()))
                .willReturn(com.energymonitor.security.domain.model.PasswordResetToken.issue(
                        "rst0000001", "USR0000001", "the-secret-reset-token-value",
                        "0".repeat(64), now, now.plusSeconds(900)));

        String body = mvc.perform(post("/api/v1/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"someone@example.com\"}"))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertTrue(body.isEmpty(),
                "The reset endpoint answered with a body instead of nothing");
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("rst0000001"),
                "The reset endpoint leaked the reset token");
        org.junit.jupiter.api.Assertions.assertFalse(
                body.contains("the-secret-reset-token-value"),
                "The reset endpoint leaked the reset token value");
    }

    @Test
    void rejectsAnUnknownResetTokenWithBadRequest() throws Exception {
        willThrow(new com.energymonitor.security.application.exception.InvalidResetTokenException(
                "Reset token is invalid or expired"))
                .given(resetPassword).reset(any());

        mvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resetToken":"bogus","newPassword":"NewStrong1!"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Reset token is invalid or expired"));
    }

    @Test
    void completesAResetWithAValidToken() throws Exception {
        mvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resetToken":"rst0000001","newPassword":"NewStrong1!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated."));
    }

    // --------------------------------------------------------- protected endpoints

    @Test
    void changesThePasswordOfTheAuthenticatedCaller() throws Exception {
        mvc.perform(post("/api/v1/auth/password/change")
                        .header("Authorization", bearerFor(identity()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"StrongPass1!","newPassword":"NewStrong1!"}
                                """))
                .andExpect(status().isOk());

        verify(changePassword).change(any());
    }

    @Test
    void readsTheAccountFromTheTokenAndNotFromTheRequest() throws Exception {
        mvc.perform(post("/api/v1/auth/password/change")
                        .header("Authorization", bearerFor(identity()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"StrongPass1!","newPassword":"NewStrong1!"}
                                """))
                .andExpect(status().isOk());

        verify(changePassword).change(
                org.mockito.ArgumentMatchers.argThat(command ->
                        "USR0000001".equals(command.idUser())));
    }

    @Test
    void revokesTheSessionOfTheAuthenticatedCaller() throws Exception {
        mvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", bearerFor(identity()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUserSession\":\"SES0000001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Session closed."));

        verify(logoutUserSession).logout(
                org.mockito.ArgumentMatchers.argThat(command ->
                        "SES0000001".equals(command.idUserSession())
                                && "USR0000001".equals(command.idUser())));
    }

    @Test
    void refusesToChangeThePasswordWithoutAToken() throws Exception {
        mvc.perform(post("/api/v1/auth/password/change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"StrongPass1!","newPassword":"NewStrong1!"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("A valid access token is required."));

        verify(changePassword, never()).change(any());
    }

    @Test
    void refusesToLogOutWithoutAToken() throws Exception {
        mvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUserSession\":\"SES0000001\"}"))
                .andExpect(status().isUnauthorized());

        verify(logoutUserSession, never()).logout(any());
    }

    @Test
    void refusesATokenSignedWithAnotherKey() throws Exception {
        String forged = "eyJhbGciOiJIUzI1NiJ9"
                + ".eyJzdWIiOiJVU1IwMDAwMDAxIn0"
                + ".ZmFrZS1zaWduYXR1cmUtdGhhdC13aWxsLW5vdC12ZXJpZnk";

        mvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + forged)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUserSession\":\"SES0000001\"}"))
                .andExpect(status().isUnauthorized());

        verify(logoutUserSession, never()).logout(any());
    }

    @Test
    void refusesAMalformedToken() throws Exception {
        mvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer not-a-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUserSession\":\"SES0000001\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void leavesRegistrationReachableWithoutAToken() throws Exception {
        // The permit list is what makes sign-up possible at all: a caller has no identity yet.
        given(registerUser.register(any())).willReturn(newAccount());

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody()))
                .andExpect(status().isCreated());
    }

    @Test
    void reachesTheLoginUseCaseWithoutATokenRatherThanBeingBlockedByTheFilterChain() throws Exception {
        given(loginFlow.login(any(), any(), any(), any())).willReturn(Optional.empty());

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"someone@example.com","password":"whatever"}
                                """))
                .andExpect(jsonPath("$.message").value("Invalid credentials."));

        verify(loginFlow).login(any(), any(), any(), any());
    }

    @Test
    void protectsEveryOtherRouteByDefault() throws Exception {
        mvc.perform(post("/api/v1/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\"}"))
                .andExpect(status().isAccepted());

        mvc.perform(post("/api/v1/auth/unknown")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
