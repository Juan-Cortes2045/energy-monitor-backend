package com.energymonitor.home.domain.model;

import java.util.Objects;

/**
 * Membership of a user in a home.
 *
 * <p>A many-to-many link between {@code security.user} and {@code home}. The {@code userId}
 * is an application-level reference (no SQL foreign key across bounded contexts).
 *
 * <p>Invariants:
 * <ul>
 *   <li>HOME-INV-001: a home always has at least one active OWNER</li>
 *   <li>HOME-INV-002: the only OWNER cannot leave the home</li>
 *   <li>HOME-INV-003: only a user with {@code canManageHome()} can remove others</li>
 *   <li>HOME-INV-004: an OWNER cannot be removed from a home</li>
 *   <li>HOME-INV-008: a user cannot have two active memberships in the same home</li>
 * </ul>
 *
 * <p>Maps to the {@code user_home} table.
 */
public class UserHome {

    private final String userId;
    private final String homeId;
    private final Role role;
    private boolean favorite;

    /**
     * Rehydrates or creates a membership. Prefer {@link #join} or {@link #becomeOwner}.
     *
     * @param userId identifier of the user, {@code VARCHAR(10)}
     * @param homeId identifier of the home, {@code VARCHAR(10)}
     * @param role   role inside the home
     * @param favorite whether the home is marked as favorite
     */
    public UserHome(String userId, String homeId, Role role, boolean favorite) {
        this.userId = Preconditions.text(userId, 10, "userId");
        this.homeId = Preconditions.text(homeId, 10, "homeId");
        this.role = Preconditions.notNull(role, "role");
        this.favorite = favorite;
    }

    /**
     * Creates a membership for a user joining a home.
     *
     * @param userId identifier of the user
     * @param homeId identifier of the home
     * @param role   role inside the home
     * @return a new membership with {@code favorite = false}
     */
    public static UserHome join(String userId, String homeId, Role role) {
        return new UserHome(userId, homeId, role, false);
    }

    /**
     * Creates the OWNER membership when a home is created.
     *
     * @param userId identifier of the creating user
     * @param homeId identifier of the new home
     * @return a new OWNER membership
     */
    public static UserHome becomeOwner(String userId, String homeId) {
        return new UserHome(userId, homeId, Role.OWNER, false);
    }

    /**
     * Creates an OWNER membership (favorite = false).
     *
     * @param userId identifier of the user
     * @param homeId identifier of the home
     * @return a new OWNER membership
     */
    public static UserHome owner(String userId, String homeId) {
        return new UserHome(userId, homeId, Role.OWNER, false);
    }

    /**
     * Creates a MEMBER membership (favorite = false).
     *
     * @param userId identifier of the user
     * @param homeId identifier of the home
     * @return a new MEMBER membership
     */
    public static UserHome member(String userId, String homeId) {
        return new UserHome(userId, homeId, Role.MEMBER, false);
    }

    /** @return identifier of the user */
    public String userId() {
        return userId;
    }

    /** @return identifier of the home */
    public String homeId() {
        return homeId;
    }

    /** @return role inside the home */
    public Role role() {
        return role;
    }

    /** @return whether the home is marked as favorite */
    public boolean isFavorite() {
        return favorite;
    }

    /**
     * Toggles the favorite flag.
     */
    public void toggleFavorite() {
        this.favorite = !this.favorite;
    }

    /**
     * Whether this membership can be removed by the given actor.
     *
     * <p>Rules:
     * <ul>
     *   <li>The actor must belong to the same home ({@code actor.homeId == this.homeId}).</li>
     *   <li>HOME-INV-003: the actor must have {@code canManageHome()}.</li>
     *   <li>HOME-INV-004: an OWNER cannot be removed.</li>
     * </ul>
     *
     * @param actor the membership of the user attempting the removal
     * @return {@code true} when the removal is allowed
     */
    public boolean canBeRemovedBy(UserHome actor) {
        Preconditions.notNull(actor, "actor");
        if (!this.homeId.equals(actor.homeId)) {
            return false;
        }
        if (!actor.getPermissions().canManageHome()) {
            return false;
        }
        return this.role != Role.OWNER;
    }

    /**
     * Marks this membership as left (soft delete).
     *
     * <p>HOME-INV-002: the only OWNER cannot leave the home. The caller must verify
     * that other owners exist before invoking this method.
     *
     * @param otherOwnersExist whether at least one other OWNER exists in the home
     * @throws IllegalStateException if this membership is OWNER and no other owners exist
     */
    public void leaveHome(boolean otherOwnersExist) {
        if (this.role == Role.OWNER && !otherOwnersExist) {
            throw new IllegalStateException("the only OWNER cannot leave the home");
        }
    }

    /**
     * Whether this membership is an OWNER.
     *
     * @return {@code true} when the role is {@link Role#OWNER}
     */
    public boolean isOwner() {
        return role == Role.OWNER;
    }

    /**
     * Returns the permissions associated with this membership's role.
     *
     * <p>Strategy pattern: the role determines the concrete {@link UserPermissions}.
     *
     * @return the permissions for this membership's role
     */
    public UserPermissions getPermissions() {
        return role == Role.OWNER ? OwnerPermissions.INSTANCE : MemberPermissions.INSTANCE;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserHome that)) {
            return false;
        }
        return userId.equals(that.userId) && homeId.equals(that.homeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, homeId);
    }

    @Override
    public String toString() {
        return "UserHome{userId='" + userId + "', homeId='" + homeId + "', role=" + role + ", favorite=" + favorite + "}";
    }
}
