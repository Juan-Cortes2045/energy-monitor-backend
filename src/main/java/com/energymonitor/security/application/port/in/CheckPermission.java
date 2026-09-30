package com.energymonitor.security.application.port.in;

import com.energymonitor.security.application.command.CheckPermissionQuery;

/**
 * Input port for authorizing a user against a permission code.
 */
public interface CheckPermission {

    /**
     * @param query the user and the permission to test
     * @return {@code true} when the user holds the permission through an enabled role
     */
    boolean check(CheckPermissionQuery query);
}