package com.energymonitor.home.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UserHomeTest {

    private UserHome createOwnerMembership() {
        return UserHome.becomeOwner("use0000001", "hom0000001");
    }

    private UserHome createMemberMembership() {
        return UserHome.join("use0000002", "hom0000001", Role.MEMBER);
    }

    @Test
    void becomeOwnerCreatesOwnerMembership() {
        UserHome membership = createOwnerMembership();
        assertEquals("use0000001", membership.userId());
        assertEquals("hom0000001", membership.homeId());
        assertEquals(Role.OWNER, membership.role());
        assertFalse(membership.isFavorite());
    }

    @Test
    void joinCreatesMemberMembership() {
        UserHome membership = createMemberMembership();
        assertEquals("use0000002", membership.userId());
        assertEquals("hom0000001", membership.homeId());
        assertEquals(Role.MEMBER, membership.role());
        assertFalse(membership.isFavorite());
    }

    @Test
    void joinWithOwnerRoleCreatesOwnerMembership() {
        UserHome membership = UserHome.join("use0000003", "hom0000001", Role.OWNER);
        assertEquals(Role.OWNER, membership.role());
    }

    @Test
    void createWithBlankUserIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> UserHome.join("", "hom0000001", Role.MEMBER));
    }

    @Test
    void createWithTooLongUserIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> UserHome.join("use00000012345", "hom0000001", Role.MEMBER));
    }

    @Test
    void createWithBlankHomeIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> UserHome.join("use0000001", "", Role.MEMBER));
    }

    @Test
    void createWithNullRoleThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new UserHome("use0000001", "hom0000001", null, false));
    }

    @Test
    void toggleFavoriteSwitchesFlag() {
        UserHome membership = createMemberMembership();
        assertFalse(membership.isFavorite());
        membership.toggleFavorite();
        assertTrue(membership.isFavorite());
        membership.toggleFavorite();
        assertFalse(membership.isFavorite());
    }

    @Test
    void isOwnerReturnsTrueForOwner() {
        assertTrue(createOwnerMembership().isOwner());
    }

    @Test
    void isOwnerReturnsFalseForMember() {
        assertFalse(createMemberMembership().isOwner());
    }

    @Test
    void getPermissionsReturnsOwnerPermissionsForOwner() {
        UserPermissions permissions = createOwnerMembership().getPermissions();
        assertTrue(permissions instanceof OwnerPermissions);
        assertTrue(permissions.canManageDevices());
        assertTrue(permissions.canManageHome());
    }

    @Test
    void getPermissionsReturnsMemberPermissionsForMember() {
        UserPermissions permissions = createMemberMembership().getPermissions();
        assertTrue(permissions instanceof MemberPermissions);
        assertFalse(permissions.canManageDevices());
        assertFalse(permissions.canManageHome());
    }

    @Test
    void canBeRemovedByAllowsMemberRemovedByOwner() {
        UserHome owner = createOwnerMembership();
        UserHome member = createMemberMembership();
        assertTrue(member.canBeRemovedBy(owner));
    }

    @Test
    void canBeRemovedByDeniesOwnerRemovedByOwner() {
        UserHome owner1 = createOwnerMembership();
        UserHome owner2 = UserHome.becomeOwner("use0000003", "hom0000001");
        assertFalse(owner1.canBeRemovedBy(owner2));
    }

    @Test
    void canBeRemovedByDeniesMemberRemovedByMember() {
        UserHome member1 = createMemberMembership();
        UserHome member2 = UserHome.join("use0000004", "hom0000001", Role.MEMBER);
        assertFalse(member1.canBeRemovedBy(member2));
    }

    @Test
    void canBeRemovedByDeniesActorFromDifferentHome() {
        UserHome owner = createOwnerMembership();
        UserHome member = createMemberMembership();
        UserHome otherHomeMember = UserHome.join("use0000002", "hom0000002", Role.MEMBER);
        assertFalse(member.canBeRemovedBy(otherHomeMember));
    }

    @Test
    void canBeRemovedByDeniesRemovalByNullActor() {
        UserHome member = createMemberMembership();
        assertThrows(IllegalArgumentException.class, () -> member.canBeRemovedBy(null));
    }

    @Test
    void leaveHomeAllowsMemberToLeave() {
        UserHome member = createMemberMembership();
        member.leaveHome(false);
    }

    @Test
    void leaveHomeAllowsOwnerToLeaveWhenOtherOwnersExist() {
        UserHome owner = createOwnerMembership();
        owner.leaveHome(true);
    }

    @Test
    void leaveHomeDeniesOnlyOwnerToLeave() {
        UserHome owner = createOwnerMembership();
        assertThrows(IllegalStateException.class, () -> owner.leaveHome(false));
    }

    @Test
    void ownerFactoryCreatesOwnerMembership() {
        UserHome membership = UserHome.owner("use0000001", "hom0000001");
        assertEquals(Role.OWNER, membership.role());
        assertFalse(membership.isFavorite());
    }

    @Test
    void memberFactoryCreatesMemberMembership() {
        UserHome membership = UserHome.member("use0000001", "hom0000001");
        assertEquals(Role.MEMBER, membership.role());
        assertFalse(membership.isFavorite());
    }

    @Test
    void equalsBasedOnUserIdAndHomeId() {
        UserHome m1 = createMemberMembership();
        UserHome m2 = UserHome.join("use0000002", "hom0000001", Role.OWNER);
        assertEquals(m1, m2);
    }

    @Test
    void notEqualsWhenDifferentUserId() {
        UserHome m1 = createMemberMembership();
        UserHome m2 = UserHome.join("use0000003", "hom0000001", Role.MEMBER);
        assertNotEquals(m1, m2);
    }

    @Test
    void notEqualsWhenDifferentHomeId() {
        UserHome m1 = createMemberMembership();
        UserHome m2 = UserHome.join("use0000002", "hom0000002", Role.MEMBER);
        assertNotEquals(m1, m2);
    }

    @Test
    void notEqualsWhenDifferentType() {
        UserHome membership = createMemberMembership();
        assertNotEquals(membership, "not a membership");
    }
}
