package com.energymonitor.measurement.adapter.out.persistence.entity;

import com.energymonitor.measurement.api.RiskConsumption;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA mapping of the {@code consumption_level} table.
 *
 * <p>{@code name} is a MySQL {@code ENUM} column mapped as a string, the same approach
 * {@code user_home.role} already uses.
 */
@Entity
@Table(name = "consumption_level")
public class ConsumptionLevelEntity extends BaseAuditEntity {

    @Id
    @Column(name = "id_consumption_level", nullable = false, length = 10)
    private String idConsumptionLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, length = 10, unique = true)
    private RiskConsumption name;

    @Column(name = "description", nullable = false, length = 200)
    private String description;

    @Column(name = "min_limit", nullable = false)
    private double minLimit;

    @Column(name = "max_limit", nullable = false)
    private double maxLimit;

    public ConsumptionLevelEntity() {
    }

    public ConsumptionLevelEntity(String idConsumptionLevel, RiskConsumption name,
                                  String description, double minLimit, double maxLimit) {
        this.idConsumptionLevel = idConsumptionLevel;
        this.name = name;
        this.description = description;
        this.minLimit = minLimit;
        this.maxLimit = maxLimit;
    }

    public String getIdConsumptionLevel() {
        return idConsumptionLevel;
    }

    public void setIdConsumptionLevel(String idConsumptionLevel) {
        this.idConsumptionLevel = idConsumptionLevel;
    }

    public RiskConsumption getName() {
        return name;
    }

    public void setName(RiskConsumption name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getMinLimit() {
        return minLimit;
    }

    public void setMinLimit(double minLimit) {
        this.minLimit = minLimit;
    }

    public double getMaxLimit() {
        return maxLimit;
    }

    public void setMaxLimit(double maxLimit) {
        this.maxLimit = maxLimit;
    }
}
