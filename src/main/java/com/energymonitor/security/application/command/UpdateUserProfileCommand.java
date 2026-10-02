package com.energymonitor.security.application.command;

/**
 * Input of {@code UpdateUserProfile}: the mutable personal and contact data of a person,
 * plus the optional account address change.
 *
 * <p>{@code null} fields are left unchanged, except the contact fields which, when the whole
 * triple is non-empty, clear what they receive as {@code null}.
 *
 * @param idUser       the account whose profile is edited
 * @param name         new first name, {@code null} to keep
 * @param lastName     new last name, {@code null} to keep
 * @param cellphone    new phone or {@code null}
 * @param profileImage new profile image or {@code null}
 * @param newEmail     delivery hint; the email is never taken from any of the other fields
 * @param ipAddress    optional origin of the request, recorded in the audit trail
 */
public record UpdateUserProfileCommand(String idUser, String name, String lastName,
                                       String cellphone, String profileImage,
                                       String newEmail, String ipAddress) {
}