package com.energymonitor.security.application.port.out;

import com.energymonitor.security.api.AccountDeleted;

/**
 * Output port for telling other modules about account lifecycle changes.
 */
public interface AccountEventPublisherPort {

    /**
     * @param event the deletion that just happened
     */
    void accountDeleted(AccountDeleted event);
}
