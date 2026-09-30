package com.energymonitor.security.domain.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Personal identity of a human being. Aggregate root of its own small aggregate.
 *
 * <p>Holds personal data only. It deliberately stores no credentials, tokens or account state:
 * authentication belongs to {@link User} (INV-002). This separation is what lets the same
 * person's data exist without an account.
 *
 * <p>Maps to the {@code person} table.
 */
public class Person {

    private static final int NAME_MAX = 100;
    private static final int CELLPHONE_MAX = 15;
    private static final int ADDRESS_MAX = 200;
    private static final int PROFILE_IMAGE_MAX = 255;

    private final String idPerson;
    private String name;
    private String lastName;
    private String cellphone;
    private String address;
    private String profileImage;

    /**
     * @param idPerson     identifier, {@code VARCHAR(10)}
     * @param name         first name, required (INV-001)
     * @param lastName     last name, required (INV-001)
     * @param cellphone    optional
     * @param address      optional
     * @param profileImage optional
     */
    public Person(String idPerson, String name, String lastName,
                  String cellphone, String address, String profileImage) {
        this.idPerson = Preconditions.text(idPerson, "idPerson");
        this.name = Preconditions.text(name, NAME_MAX, "name");
        this.lastName = Preconditions.text(lastName, NAME_MAX, "lastName");
        this.cellphone = Preconditions.optionalText(cellphone, CELLPHONE_MAX, "cellphone");
        this.address = Preconditions.optionalText(address, ADDRESS_MAX, "address");
        this.profileImage = Preconditions.optionalText(profileImage, PROFILE_IMAGE_MAX, "profileImage");
    }

    /** @return the identifier */
    public String idPerson() {
        return idPerson;
    }

    /** @return first name */
    public String name() {
        return name;
    }

    /** @return last name */
    public String lastName() {
        return lastName;
    }

    /** @return phone number, empty when not provided */
    public Optional<String> cellphone() {
        return Optional.ofNullable(cellphone);
    }

    /** @return address, empty when not provided */
    public Optional<String> address() {
        return Optional.ofNullable(address);
    }

    /** @return profile image URL or path, empty when not provided */
    public Optional<String> profileImage() {
        return Optional.ofNullable(profileImage);
    }

    /**
     * Replaces the first and last name.
     *
     * @param name     new first name
     * @param lastName new last name
     */
    public void rename(String name, String lastName) {
        this.name = Preconditions.text(name, NAME_MAX, "name");
        this.lastName = Preconditions.text(lastName, NAME_MAX, "lastName");
    }

    /**
     * Replaces the optional contact data. Passing {@code null} clears a field.
     *
     * @param cellphone    new phone number or {@code null}
     * @param address      new address or {@code null}
     * @param profileImage new profile image or {@code null}
     */
    public void updateContactData(String cellphone, String address, String profileImage) {
        this.cellphone = Preconditions.optionalText(cellphone, CELLPHONE_MAX, "cellphone");
        this.address = Preconditions.optionalText(address, ADDRESS_MAX, "address");
        this.profileImage = Preconditions.optionalText(profileImage, PROFILE_IMAGE_MAX, "profileImage");
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Person person)) {
            return false;
        }
        return idPerson.equals(person.idPerson);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPerson);
    }

    @Override
    public String toString() {
        return "Person{idPerson='" + idPerson + "'}";
    }
}
