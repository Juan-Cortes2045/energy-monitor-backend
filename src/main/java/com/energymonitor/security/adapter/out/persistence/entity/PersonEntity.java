package com.energymonitor.security.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA mapping of the {@code person} table.
 *
 * <p>Plain mapping, no associations. {@code person} has no outgoing foreign keys, so there is
 * nothing to link to.
 */
@Entity
@Table(name = "person")
public class PersonEntity extends BaseAuditEntity {

    @Id
    @Column(name = "id_person", nullable = false, length = 10)
    private String idPerson;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "cellphone", length = 15)
    private String cellphone;

    @Column(name = "address", length = 200)
    private String address;

    @Column(name = "profile_image", length = 255)
    private String profileImage;

    public PersonEntity() {
    }

    public String getIdPerson() {
        return idPerson;
    }

    public void setIdPerson(String idPerson) {
        this.idPerson = idPerson;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCellphone() {
        return cellphone;
    }

    public void setCellphone(String cellphone) {
        this.cellphone = cellphone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}
