package com.energymonitor.home.application.port.out;

import com.energymonitor.home.api.HomeDeleted;

/**
 * Domain events of the home module, consumed by other modules through {@code home::api}.
 */
public interface HomeEventPort {

    void publish(HomeDeleted event);
}
