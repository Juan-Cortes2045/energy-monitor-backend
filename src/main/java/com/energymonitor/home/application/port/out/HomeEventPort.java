package com.energymonitor.home.application.port.out;

import com.energymonitor.home.api.HomeDeleted;
import com.energymonitor.home.api.HomeOwnershipTransferred;

/**
 * Domain events of the home module, consumed by other modules through {@code home::api}.
 */
public interface HomeEventPort {

    void publish(HomeDeleted event);

    void publish(HomeOwnershipTransferred event);
}
