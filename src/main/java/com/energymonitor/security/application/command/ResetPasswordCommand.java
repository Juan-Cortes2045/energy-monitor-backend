package com.energymonitor.security.application.command;

/**
 * Input of {@code ResetPassword}: redeems a previously issued reset token.
 *
 * @param resetToken  the opaque token value received out of band
 * @param newPassword the new plain-text password, validated against the policy
 * @param ipAddress   optional origin of the request, recorded in the audit trail
 */
public record ResetPasswordCommand(String resetToken, String newPassword, String ipAddress) {
}