package com.energymonitor.security.adapter.in.web;

import com.energymonitor.security.adapter.in.web.dto.AccountResponse;
import com.energymonitor.security.adapter.in.web.dto.ChangePasswordRequest;
import com.energymonitor.security.adapter.in.web.dto.ForgotPasswordRequest;
import com.energymonitor.security.adapter.in.web.dto.LoginRequest;
import com.energymonitor.security.adapter.in.web.dto.LoginResponse;
import com.energymonitor.security.adapter.in.web.dto.LogoutRequest;
import com.energymonitor.security.adapter.in.web.dto.MessageResponse;
import com.energymonitor.security.adapter.in.web.dto.RefreshRequest;
import com.energymonitor.security.adapter.in.web.dto.RefreshResponse;
import com.energymonitor.security.adapter.in.web.dto.RegisterRequest;
import com.energymonitor.security.adapter.in.web.dto.ResetPasswordRequest;
import com.energymonitor.security.application.command.ChangePasswordCommand;
import com.energymonitor.security.application.command.CreatePasswordResetTokenCommand;
import com.energymonitor.security.application.command.RegisterUserCommand;
import com.energymonitor.security.application.command.ResetPasswordCommand;
import com.energymonitor.security.application.command.LogoutUserSessionCommand;
import com.energymonitor.security.application.command.RefreshSessionCommand;
import com.energymonitor.security.application.exception.UserNotFoundException;
import com.energymonitor.security.application.port.in.ChangePassword;
import com.energymonitor.security.application.port.in.CreatePasswordResetToken;
import com.energymonitor.security.application.port.in.RegisterUser;
import com.energymonitor.security.application.port.in.ResetPassword;
import com.energymonitor.security.application.port.in.LogoutUserSession;
import com.energymonitor.security.application.port.in.RefreshSession;
import com.energymonitor.security.application.result.RefreshSessionResult;
import com.energymonitor.security.infrastructure.JwtTokenIssuer;
import com.energymonitor.security.infrastructure.SecurityLoginFlow;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP adapter for authentication, sessions and password management.
 *
 * <p>The controller is a translator and nothing more. It turns a request body into an
 * application command, hands that command to an input port, and turns what comes back into a
 * response DTO. It holds no business rule: every decision belongs to a use case, and every
 * mapping is an explicit field-by-field copy, which is what keeps aggregates, password hashes
 * and persistence entities out of responses.
 *
 * <p>The account behind the authenticated endpoints is read from the validated token's
 * subject rather than from the request, so a caller can only ever act on its own account.
 *
 * <h2>Refresh and its transport</h2>
 *
 * <p>{@code POST /refresh} exchanges the secret issued at login for a new access token and a
 * new secret. The refresh token travels in the request and response bodies, matching
 * {@code LoginResponse}, and the API therefore carries no cookies and no CSRF tokens: the
 * credential is sent explicitly by the client rather than attached automatically by the
 * browser, so there is nothing for a cross-site request to ride on. The trade-off is that the
 * secret is reachable by client-side code, which makes the client's own storage discipline part
 * of the security posture. See {@code LoginResponse} for that reasoning in full.
 *
 * <h2>Known limitation of this surface</h2>
 *
 * <p>One part of the authentication flow is deliberately incomplete, recorded here so that
 * reading the code does not suggest otherwise:
 *
 * <ul>
 *   <li><strong>Password recovery is not completable.</strong> {@code POST /password/forgot}
 *       persists a reset token that no channel delivers to anyone, so {@code POST /password/reset}
 *       cannot currently be reached by a real user. The missing piece is an outbound
 *       notification adapter, which is out of scope for this phase.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String BEARER = "Bearer";

    /**
     * Neutral acknowledgement for the reset request. Deliberately identical whether or not
     * the account exists, so the endpoint cannot be used to discover registered addresses.
     *
     * <p>The wording also hides the fact that the token is currently undeliverable, which is
     * deliberate and correct: the caller has no way to act on that knowledge either way.
     */
    private static final String RESET_REQUESTED =
            "If the account exists, a password reset has been requested.";

    private final RegisterUser registerUser;
    private final SecurityLoginFlow loginFlow;
    private final LogoutUserSession logoutUserSession;
    private final RefreshSession refreshSession;
    private final JwtTokenIssuer tokenIssuer;
    private final CreatePasswordResetToken createPasswordResetToken;
    private final ResetPassword resetPassword;
    private final ChangePassword changePassword;

    public AuthController(RegisterUser registerUser, SecurityLoginFlow loginFlow,
                          LogoutUserSession logoutUserSession, RefreshSession refreshSession,
                          JwtTokenIssuer tokenIssuer,
                          CreatePasswordResetToken createPasswordResetToken,
                          ResetPassword resetPassword, ChangePassword changePassword) {
        this.registerUser = registerUser;
        this.loginFlow = loginFlow;
        this.logoutUserSession = logoutUserSession;
        this.refreshSession = refreshSession;
        this.tokenIssuer = tokenIssuer;
        this.createPasswordResetToken = createPasswordResetToken;
        this.resetPassword = resetPassword;
        this.changePassword = changePassword;
    }

    /**
     * Registers an account. Open by design: a caller has no identity yet.
     *
     * @param request the registration body
     * @param servlet used only for the caller's address
     * @return the created account, without its password hash
     */
    @PostMapping("/register")
    public ResponseEntity<AccountResponse> register(@Valid @RequestBody RegisterRequest request,
                                                    HttpServletRequest servlet) {
        var account = registerUser.register(new RegisterUserCommand(request.email(),
                request.password(), request.name(), request.lastName(), request.cellphone(),
                request.address(), request.profileImage(), clientIp(servlet)));
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    /**
     * Authenticates and issues an access token plus a refresh session.
     *
     * <p>Every rejection produces the same 401 with the same body. The use case records the
     * precise reason in the typed error trail, but telling the caller whether the address is
     * unknown or the password wrong would turn this endpoint into an account-enumeration
     * oracle, so that distinction stops at the server and the failure is re-thrown as an
     * {@link AuthenticationRejectedException} for the shared handler to render.
     *
     * @param request the credentials
     * @param servlet used for the caller's address and user agent
     * @return the tokens and account
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest servlet) {
        SecurityLoginFlow.LoginOutcome login = loginFlow.login(request.email(), request.password(),
                clientIp(servlet), servlet.getHeader("User-Agent"))
                .orElseThrow(AuthenticationRejectedException::new);
        return ResponseEntity.ok(new LoginResponse(login.accessToken(), login.refreshToken(),
                BEARER, login.expiresInSeconds(), AccountResponse.from(login.account())));
    }

    /**
     * Exchanges a refresh token for a new pair of credentials.
     *
     * <p>Reachable without an access token, which is the whole point: this is what a client
     * calls once its access token has expired, so requiring one would make it useless exactly
     * when it is needed.
     *
     * <p>The controller does no refresh-token work of its own. It does not look up a
     * generation, compute a digest, walk a family or decide whether a token was replayed; all
     * of that belongs to {@link RefreshSession}, and the identity used to sign the new access
     * token comes back from that use case rather than from a lookup here. That is what makes
     * the endpoint unable to be pointed at somebody else's account.
     *
     * <p>Every failure leaves as the same 401 with the same body. An unknown, expired or revoked
     * secret and a detected replay are different situations internally, and the difference is
     * written to the audit trail, but a client learns only that the authentication failed.
     * Distinguishing them in the response would tell an attacker which tokens are worth
     * replaying.
     *
     * @param request the presented secret
     * @param servlet used for the caller's address
     * @return the renewed credentials
     */
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request,
                                                    HttpServletRequest servlet) {
        RefreshSessionResult result = refreshSession.refresh(
                new RefreshSessionCommand(request.refreshToken(), clientIp(servlet)));
        if (!result.issued()) {
            throw new AuthenticationRejectedException();
        }
        return ResponseEntity.ok(new RefreshResponse(
                tokenIssuer.issue(result.identity()), result.rawRefreshToken()));
    }

    /**
     * Logs out of a session. Requires a valid access token.
     *
     * <p>The session is closed, not revoked. Walking away is not a security event, and the row
     * stays readable as an ordinary departure rather than as a suspected compromise.
     *
     * @param request       names the session to close
     * @param authentication the validated caller, whose subject is the account identifier
     * @param servlet       used for the caller's address
     * @return an acknowledgement
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@Valid @RequestBody LogoutRequest request,
                                                  Authentication authentication,
                                                  HttpServletRequest servlet) {
        logoutUserSession.logout(new LogoutUserSessionCommand(request.idUserSession(),
                authentication.getName(), clientIp(servlet)));
        return ResponseEntity.ok(new MessageResponse("Session revoked."));
    }

    /**
     * Requests a password reset.
     *
     * <p>What this endpoint does: looks the account up, mints a reset token, persists it, and
     * answers 202 without revealing whether the account exists.
     *
     * <p><strong>The flow is not completable by a real user today.</strong> The token is
     * persisted but never returned, and no delivery channel exists for it: this application
     * has no mail sender, no SMS sender and no notification adapter, and no endpoint that
     * would let a caller obtain the token. A user calling this endpoint therefore receives an
     * acknowledgement and no token, and cannot reach {@code POST /password/reset}.
     *
     * <p>Not returning the token is the correct security decision and is not the defect. Echoing
     * it would hand a working credential to whoever asked, turning this endpoint into an
     * account-takeover primitive. The gap is the missing delivery channel, not the absence of
     * the token in this response.
     *
     * <p>Building that channel is out of scope for this phase. It requires an outbound
     * notification adapter and a decision about the message content and the link format, and
     * it belongs to a later phase; this endpoint is complete as far as token issuance goes and
     * is expected to be paired with that adapter rather than altered by it.
     *
     * @param request the account address
     * @return the same acknowledgement whether or not the account exists
     */
    @PostMapping("/password/forgot")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        try {
            createPasswordResetToken.create(new CreatePasswordResetTokenCommand(request.email()));
        } catch (UserNotFoundException unknownAccount) {
            // Swallowed on purpose: reporting it would reveal which addresses are registered.
        }
        return ResponseEntity.accepted().body(new MessageResponse(RESET_REQUESTED));
    }

    /**
     * Completes a password reset with a token obtained through the reset channel.
     *
     * <p>This endpoint works: it looks the token up, rejects it when it is unknown, expired or
     * already used, checks the replacement against the configured policy, and commits the new
     * hash, the token consumption and the audit row in one transaction.
     *
     * <p>It is reachable only by a caller who already holds a valid token, and today no such
     * token can reach a user, because {@code POST /password/forgot} has no delivery channel
     * (see that method). The endpoint being public is therefore necessary but not yet
     * sufficient for the recovery flow to work end to end; the missing piece is token
     * delivery, not this handler.
     *
     * <p>The token is accepted here because completing a reset is the entire purpose of the
     * request, and it is never echoed in the response.
     *
     * @param request the token and the replacement password
     * @param servlet used for the caller's address
     * @return an acknowledgement
     */
    @PostMapping("/password/reset")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request, HttpServletRequest servlet) {
        resetPassword.reset(new ResetPasswordCommand(request.resetToken(), request.newPassword(),
                clientIp(servlet)));
        return ResponseEntity.ok(new MessageResponse("Password updated."));
    }

    /**
     * Changes the caller's own password. Requires a valid access token.
     *
     * @param request       the current and replacement passwords
     * @param authentication the validated caller
     * @param servlet       used for the caller's address
     * @return an acknowledgement
     */
    @PostMapping("/password/change")
    public ResponseEntity<MessageResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request, Authentication authentication,
            HttpServletRequest servlet) {
        changePassword.change(new ChangePasswordCommand(authentication.getName(),
                request.currentPassword(), request.newPassword(), clientIp(servlet)));
        return ResponseEntity.ok(new MessageResponse("Password changed."));
    }

    /**
     * The caller's address, forwarded headers included, or {@code null} when unknown.
     *
     * <p>The use cases accept a nullable address precisely so they never have to care whether
     * the deployment sits behind a proxy.
     *
     * @param servlet the current request
     * @return the address to record in the audit trail
     */
    private String clientIp(HttpServletRequest servlet) {
        String forwarded = servlet.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return servlet.getRemoteAddr();
    }
}
