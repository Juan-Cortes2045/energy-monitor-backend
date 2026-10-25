package com.energymonitor.home.application.port.in;

/**
 * Input port for the device and measurement services asking what a caller may do in a home: list
 * its devices or read its consumption as a member, or change its devices as an owner.
 */
public interface DescribeHomeAccess {

    /**
     * @param userId the caller, from the device service's access token
     * @param homeId the home in the path of the device request
     * @return whether the home exists and the caller belongs to it or owns it
     */
    HomeAccess describe(String userId, String homeId);

    /**
     * What a caller may do in a home. {@code member} and {@code owner} are only true when the
     * home exists, and {@code owner} only when the caller is a member.
     *
     * @param exists whether the home exists and is active
     * @param member whether the caller is an active member, owners included
     * @param owner  whether the caller is an active OWNER
     */
    record HomeAccess(boolean exists, boolean member, boolean owner) {
    }
}
