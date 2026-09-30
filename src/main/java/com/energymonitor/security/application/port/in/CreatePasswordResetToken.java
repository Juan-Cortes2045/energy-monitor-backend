package com.energymonitor.security.application.port.in;

import com.energymonitor.security.application.command.CreatePasswordResetTokenCommand;
import com.energymonitor.security.domain.model.PasswordResetToken;

/**
 * Input port for issuing a password-recovery token.
 */
public interface CreatePasswordResetToken {

    /**
     * @param command the account to recover
     * @return the issued token, whose value is delivered out of band
     */
    PasswordResetToken create(CreatePasswordResetTokenCommand command);
}