package com.energymonitor.home.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA mapping of the {@code home_type} table.
 */
@Entity
@Table(name = "home_type")
public class HomeTypeEntity extends BaseAuditEntity {

    @Id
    @Column(name = "id_home_type", nullable = false, length = 10)
    private String idHomeType;

    @Column(name = "name", nullable = false, length = 50, unique = true)
    private String name;

    public HomeTypeEntity() {
    }

    public HomeTypeEntity(String idHomeType, String name) {
        this.idHomeType = idHomeType;
        this.name = name;
    }

    public String getIdHomeType() {
        return idHomeType;
    }

    public void setIdHomeType(String idHomeType) {
        this.idHomeType = idHomeType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
