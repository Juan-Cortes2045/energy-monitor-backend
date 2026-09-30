package com.energymonitor.security.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of a password reset.
 *
 * <p>The reset token is accepted here because completing the reset is its entire purpose. It
 * is never echoed in a response, and the endpoint that mints it never returns it either.
 *
 * @param resetToken the token received through the reset channel
 * @param newPassword the plain-text replacement, hashed by the password hasher port
 */
public record ResetPasswordRequest(
        @NotBlank @Size(max = 100) String resetToken,
        @NotBlank @Size(max = 255) String newPassword) {
}
