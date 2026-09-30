package com.energymonitor.home.domain.model;

import java.util.Objects;

/**
 * Consumption thresholds of a home (1:1 composition with {@link Home}).
 *
 * <p>Each home has exactly one row of thresholds. The limits are in kWh.
 *
 * <p>Invariants:
 * <ul>
 *   <li>HOME-INV-005: {@code dailyLimit > 0}</li>
 *   <li>HOME-INV-006: {@code monthlyLimit > 0}</li>
 *   <li>HOME-INV-007: {@code dailyLimit <= monthlyLimit}</li>
 *   <li>HOME-INV-014: when updated manually, {@code useSystemDefault} becomes {@code false}</li>
 * </ul>
 *
 * <p>Maps to the {@code home_thresholds} table.
 */
public class HomeThresholds {

    private final String idThreshold;
    private final String homeId;
    private double dailyLimit;
    private double monthlyLimit;
    private boolean useSystemDefault;

    /**
     * Rehydrates or creates thresholds. Prefer {@link #create} for new thresholds.
     *
     * @param idThreshold      identifier, {@code VARCHAR(10)}
     * @param homeId           identifier of the owning home, {@code VARCHAR(10)}
     * @param dailyLimit       daily limit in kWh, must be positive (HOME-INV-005)
     * @param monthlyLimit     monthly limit in kWh, must be positive (HOME-INV-006)
     * @param useSystemDefault whether the limits come from system defaults
     */
    public HomeThresholds(String idThreshold, String homeId, double dailyLimit,
                          double monthlyLimit, boolean useSystemDefault) {
        this.idThreshold = Preconditions.text(idThreshold, 10, "idThreshold");
        this.homeId = Preconditions.text(homeId, 10, "homeId");
        this.dailyLimit = Preconditions.positive(dailyLimit, "dailyLimit");
        this.monthlyLimit = Preconditions.positive(monthlyLimit, "monthlyLimit");
        if (dailyLimit > monthlyLimit) {
            throw new IllegalArgumentException("dailyLimit must be less than or equal to monthlyLimit");
        }
        this.useSystemDefault = useSystemDefault;
    }

    /**
     * Creates new thresholds for a home.
     *
     * @param idThreshold      identifier
     * @param homeId           identifier of the owning home
     * @param dailyLimit       daily limit in kWh
     * @param monthlyLimit     monthly limit in kWh
     * @param useSystemDefault whether the limits come from system defaults
     * @return new thresholds
     */
    public static HomeThresholds create(String idThreshold, String homeId, double dailyLimit,
                                        double monthlyLimit, boolean useSystemDefault) {
        return new HomeThresholds(idThreshold, homeId, dailyLimit, monthlyLimit, useSystemDefault);
    }

    /** @return the identifier */
    public String idThreshold() {
        return idThreshold;
    }

    /** @return identifier of the owning home */
    public String homeId() {
        return homeId;
    }

    /** @return daily limit in kWh */
    public double dailyLimit() {
        return dailyLimit;
    }

    /** @return monthly limit in kWh */
    public double monthlyLimit() {
        return monthlyLimit;
    }

    /** @return whether the limits come from system defaults */
    public boolean isUseSystemDefault() {
        return useSystemDefault;
    }

    /**
     * Updates the limits manually.
     *
     * <p>Sets {@code useSystemDefault = false} (HOME-INV-014).
     *
     * @param daily   new daily limit in kWh, must be positive (HOME-INV-005)
     * @param monthly new monthly limit in kWh, must be positive (HOME-INV-006)
     * @throws IllegalArgumentException if {@code daily > monthly} (HOME-INV-007)
     */
    public void update(double daily, double monthly) {
        Preconditions.positive(daily, "dailyLimit");
        Preconditions.positive(monthly, "monthlyLimit");
        if (daily > monthly) {
            throw new IllegalArgumentException("dailyLimit must be less than or equal to monthlyLimit");
        }
        this.dailyLimit = daily;
        this.monthlyLimit = monthly;
        this.useSystemDefault = false;
    }

    /**
     * Resets the limits to system defaults.
     *
     * <p>Sets {@code useSystemDefault = true}.
     *
     * @param daily   default daily limit in kWh
     * @param monthly default monthly limit in kWh
     */
    public void resetToDefaults(double daily, double monthly) {
        Preconditions.positive(daily, "dailyLimit");
        Preconditions.positive(monthly, "monthlyLimit");
        if (daily > monthly) {
            throw new IllegalArgumentException("dailyLimit must be less than or equal to monthlyLimit");
        }
        this.dailyLimit = daily;
        this.monthlyLimit = monthly;
        this.useSystemDefault = true;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof HomeThresholds that)) {
            return false;
        }
        return idThreshold.equals(that.idThreshold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idThreshold);
    }

    @Override
    public String toString() {
        return "HomeThresholds{idThreshold='" + idThreshold + "', homeId='" + homeId + "', dailyLimit=" + dailyLimit + ", monthlyLimit=" + monthlyLimit + "}";
    }
}
