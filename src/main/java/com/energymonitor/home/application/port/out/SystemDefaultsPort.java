package com.energymonitor.home.application.port.out;

/**
 * Output port for reading the system-wide default values used when a home is created
 * with {@code useSystemDefault = true}.
 */
public interface SystemDefaultsPort {

    /**
     * @return the default daily limit in kWh
     */
    double defaultDailyLimit();

    /**
     * @return the default monthly limit in kWh
     */
    double defaultMonthlyLimit();
}
