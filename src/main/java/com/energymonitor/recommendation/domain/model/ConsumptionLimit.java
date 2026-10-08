package com.energymonitor.recommendation.domain.model;

/**
 * The consumption limit the owner chose for the home: daily or monthly, never both.
 *
 * @param kwh limit in kWh, greater than zero
 */
public record ConsumptionLimit(Period period, double kwh) {

    public enum Period {
        DAILY,
        MONTHLY
    }
}
