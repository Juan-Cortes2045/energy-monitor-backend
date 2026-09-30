package com.energymonitor.home.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA mapping of the {@code home} table.
 */
@Entity
@Table(name = "home")
public class HomeEntity extends BaseAuditEntity {

    @Id
    @Column(name = "id_home", nullable = false, length = 10)
    private String idHome;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "home_type_id", nullable = false, length = 10)
    private String homeTypeId;

    @Column(name = "address", nullable = false, length = 200)
    private String address;

    @Column(name = "access_code", nullable = false, length = 8, unique = true)
    private String accessCode;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "creation_date", nullable = false)
    private Instant creationDate;

    public HomeEntity() {
    }

    public HomeEntity(String idHome, String name, String homeTypeId, String address,
                     String accessCode, String description, Instant creationDate) {
        this.idHome = idHome;
        this.name = name;
        this.homeTypeId = homeTypeId;
        this.address = address;
        this.accessCode = accessCode;
        this.description = description;
        this.creationDate = creationDate;
    }

    public String getIdHome() {
        return idHome;
    }

    public void setIdHome(String idHome) {
        this.idHome = idHome;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHomeTypeId() {
        return homeTypeId;
    }

    public void setHomeTypeId(String homeTypeId) {
        this.homeTypeId = homeTypeId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Instant creationDate) {
        this.creationDate = creationDate;
    }
}
