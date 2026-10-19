package com.energymonitor.device.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "appliance_type")
public class ApplianceTypeEntity extends BaseAuditEntity {

    @Id
    @Column(name = "id_appliance_type", nullable = false, length = 10)
    private String idApplianceType;

    @Column(name = "name", nullable = false, length = 50, unique = true)
    private String name;

    public String getIdApplianceType() {
        return idApplianceType;
    }

    public void setIdApplianceType(String idApplianceType) {
        this.idApplianceType = idApplianceType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
