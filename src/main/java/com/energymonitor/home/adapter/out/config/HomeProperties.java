package com.energymonitor.home.adapter.out.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Home Management module.
 *
 * <p>Values can be overridden via {@code home.defaults.daily-limit} and
 * {@code home.defaults.monthly-limit} in the application configuration.
 */
@ConfigurationProperties(prefix = "home.defaults")
public class HomeProperties {

    /** Default daily limit in kWh. */
    private double dailyLimit = 6.0;

    /** Default monthly limit in kWh. */
    private double monthlyLimit = 180.0;

    public double getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(double dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public double getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(double monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }
}
