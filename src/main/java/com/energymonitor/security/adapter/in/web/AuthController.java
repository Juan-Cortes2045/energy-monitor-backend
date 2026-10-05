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
 *   <li><strong>Password recovery cannot be completed by a real user.</strong>
 *       {@code POST /password/forgot} mints a token, stores its hash and hands the secret to
 *       {@link com.energymonitor.security.application.port.out.PasswordResetDeliveryPort}, but
 *       the only implementation shipped records the delivery instead of performing it, so
 *       {@code POST /password/reset} has no token to redeem yet. The missing piece is an
 *       outbound notification adapter, which is out of scope for this phase.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String BEARER = "Bearer";

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
                request.password(), request.name(), request.lastName(),
                request.profileImage(), clientIp(servlet)));
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
        return ResponseEntity.ok(new MessageResponse("Session closed."));
    }

    /**
     * Requests a password reset.
     *
     * <p>What this endpoint does: looks the account up, mints a reset token, hashes it, stores
     * the hash, and answers 202. The response carries no body at all - not a message, and
     * certainly not the token.
     *
     * <p>Silence is the point. An acknowledgement whose wording is the same for every caller
     * says nothing about whether the address is registered, and an empty body says even less
     * than a fixed sentence could. What the caller learns is that the request was received,
     * which is all the endpoint is ever allowed to tell them.
     *
     * <p>The token never comes back here. Returning it would hand a working credential to
     * whoever asked and turn this endpoint into an account-takeover primitive. It leaves the
     * server through {@link com.energymonitor.security.application.port.out.PasswordResetDeliveryPort}
     * instead, an outbound channel the use case hands the secret to, and the database keeps
     * only its hash. The controller never sees the secret at all: the use case's return value is
     * discarded rather than translated into a DTO.
     *
     * <p>The delivery itself is not built yet: the shipped
     * {@link com.energymonitor.security.application.port.out.PasswordResetDeliveryPort}
     * implementation records the attempt instead of sending anything, so the handler is complete
     * while the recovery flow is not. Making the flow work means implementing that port against
     * the deployment's mail, SMS or push infrastructure; it does not mean changing this handler,
     * and it must not mean echoing the token to get around the missing channel.
     *
     * @param request the account address
     * @return 202 with an empty body, whether or not the account exists
     */
    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        try {
            createPasswordResetToken.create(new CreatePasswordResetTokenCommand(request.email()));
        } catch (UserNotFoundException unknownAccount) {
            // Swallowed on purpose: reporting it would reveal which addresses are registered.
        }
        return ResponseEntity.accepted().build();
    }

    /**
     * Completes a password reset with a token obtained through the reset channel.
     *
     * <p>This endpoint works: it hashes the presented token, looks the hash up, rejects it when
     * it is unknown, expired or already used, checks the replacement against the configured
     * policy, and commits the new hash, the token consumption and the audit row in one
     * transaction.
     *
     * <p>It is reachable only by a caller who already holds a valid token, and today no such
     * token can reach a user, because {@code POST /password/forgot} has no delivery channel
     * (see that method). The endpoint being public is therefore necessary but not yet
     * sufficient for the recovery flow to work end to end; the missing piece is token
     * delivery, not this handler.
     *
     * <p>The token is accepted here because completing a reset is the entire purpose of the
     * request. It is hashed before anything else touches it and is never echoed in the response.
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
