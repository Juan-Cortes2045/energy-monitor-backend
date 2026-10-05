package com.energymonitor.measurement.domain.model;

import com.energymonitor.measurement.api.RiskConsumption;
import java.util.Objects;

/**
 * Numerical classification band of a consumption value (read-only catalog).
 *
 * <p>Each level names a {@link RiskConsumption} and the {@code [minLimit, maxLimit)} range
 * of active power that falls into it. The upper bound is exclusive so adjacent ranges can
 * share a boundary value without overlapping (MEAS-INV-010, enforced at application level
 * per the {@code measurement-002} changeset comment).
 *
 * <p>Invariants:
 * <ul>
 *   <li>MEAS-INV-008: {@code minLimit >= 0}</li>
 *   <li>MEAS-INV-009: {@code maxLimit > minLimit}</li>
 *   <li>MEAS-INV-010: ranges of different levels must not overlap (application level)</li>
 *   <li>MEAS-INV-011: {@code description} is required, {@code VARCHAR(200)}</li>
 * </ul>
 *
 * <p>Maps to the {@code consumption_level} table.
 */
public class ConsumptionLevel {

    private final String idConsumptionLevel;
    private final RiskConsumption name;
    private final String description;
    private final double minLimit;
    private final double maxLimit;

    /**
     * Rehydrates or creates a consumption level. Prefer {@link #create} for new levels.
     *
     * @param idConsumptionLevel identifier, {@code VARCHAR(10)}
     * @param name               risk classification
     * @param description        human-readable description, {@code VARCHAR(200)}
     * @param minLimit           inclusive lower bound, must not be negative (MEAS-INV-008)
     * @param maxLimit           exclusive upper bound, must exceed {@code minLimit} (MEAS-INV-009)
     */
    public ConsumptionLevel(String idConsumptionLevel, RiskConsumption name, String description,
                            double minLimit, double maxLimit) {
        this.idConsumptionLevel = Preconditions.text(idConsumptionLevel, 10, "idConsumptionLevel");
        this.name = Preconditions.notNull(name, "name");
        this.description = Preconditions.text(description, 200, "description");
        this.minLimit = Preconditions.notNegative(minLimit, "minLimit");
        if (maxLimit <= minLimit) {
            throw new IllegalArgumentException("maxLimit must be greater than minLimit");
        }
        this.maxLimit = maxLimit;
    }

    /**
     * Creates a new consumption level.
     *
     * @param idConsumptionLevel identifier
     * @param name               risk classification
     * @param description        human-readable description
     * @param minLimit           inclusive lower bound
     * @param maxLimit           exclusive upper bound
     * @return a new consumption level
     */
    public static ConsumptionLevel create(String idConsumptionLevel, RiskConsumption name,
                                          String description, double minLimit, double maxLimit) {
        return new ConsumptionLevel(idConsumptionLevel, name, description, minLimit, maxLimit);
    }

    /** @return the identifier */
    public String idConsumptionLevel() {
        return idConsumptionLevel;
    }

    /** @return the risk classification */
    public RiskConsumption name() {
        return name;
    }

    /** @return the human-readable description */
    public String description() {
        return description;
    }

    /** @return the inclusive lower bound */
    public double minLimit() {
        return minLimit;
    }

    /** @return the exclusive upper bound */
    public double maxLimit() {
        return maxLimit;
    }

    /**
     * Whether an active power value falls into this level.
     *
     * <p>The range is {@code [minLimit, maxLimit)}: the upper bound is exclusive so that
     * adjacent levels never classify the same value twice.
     *
     * @param activePower the value to classify
     * @return {@code true} when the value belongs to this level
     */
    public boolean contains(double activePower) {
        return activePower >= minLimit && activePower < maxLimit;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ConsumptionLevel that)) {
            return false;
        }
        return idConsumptionLevel.equals(that.idConsumptionLevel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idConsumptionLevel);
    }

    @Override
    public String toString() {
        return "ConsumptionLevel{idConsumptionLevel='" + idConsumptionLevel + "', name=" + name + ", range=[" + minLimit + ", " + maxLimit + ")}";
    }
}
