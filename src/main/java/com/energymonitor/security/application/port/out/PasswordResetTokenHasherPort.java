package com.energymonitor.security.application.port.out;

/**
 * Output port for turning a password-reset secret into its stored form.
 *
 * <p>Separate from {@link RefreshTokenHasherPort} even though the two are served by the same
 * adapter: each port names the credential it exists for, so a use case never has to be read
 * against a port whose subject is a different token. Nothing about the digest belongs to the
 * application layer, which uses this port in exactly two places: hashing a freshly generated
 * secret before it is stored, and hashing a presented secret to find which token it is.
 */
public interface PasswordResetTokenHasherPort {

    /**
     * Hashes a password-reset secret for storage.
     *
     * <p>What the database holds is the result of this call. The secret itself is not retained
     * and is handed to the account owner only by the flow that generated it.
     *
     * @param rawToken the secret, in clear text
     * @return the stored representation of the secret
     * @throws IllegalArgumentException if the secret is null or blank
     */
    String hash(String rawToken);
}
