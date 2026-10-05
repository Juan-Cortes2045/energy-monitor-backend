package com.energymonitor.home.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA mapping of the {@code home_thresholds} table.
 */
@Entity
@Table(name = "home_thresholds")
public class HomeThresholdsEntity extends BaseAuditEntity {

    @Id
    @Column(name = "id_threshold", nullable = false, length = 10)
    private String idThreshold;

    @Column(name = "home_id", nullable = false, length = 10, unique = true)
    private String homeId;

    @Column(name = "daily_limit", nullable = false)
    private Double dailyLimit;

    @Column(name = "monthly_limit", nullable = false)
    private Double monthlyLimit;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "use_system_default", nullable = false)
    private Boolean useSystemDefault;

    public HomeThresholdsEntity() {
    }

    public HomeThresholdsEntity(String idThreshold, String homeId, Double dailyLimit,
                               Double monthlyLimit, Boolean useSystemDefault) {
        this.idThreshold = idThreshold;
        this.homeId = homeId;
        this.dailyLimit = dailyLimit;
        this.monthlyLimit = monthlyLimit;
        this.useSystemDefault = useSystemDefault;
    }

    public String getIdThreshold() {
        return idThreshold;
    }

    public void setIdThreshold(String idThreshold) {
        this.idThreshold = idThreshold;
    }

    public String getHomeId() {
        return homeId;
    }

    public void setHomeId(String homeId) {
        this.homeId = homeId;
    }

    public Double getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(Double dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public Double getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(Double monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public Boolean getUseSystemDefault() {
        return useSystemDefault;
    }

    public void setUseSystemDefault(Boolean useSystemDefault) {
        this.useSystemDefault = useSystemDefault;
    }
}
