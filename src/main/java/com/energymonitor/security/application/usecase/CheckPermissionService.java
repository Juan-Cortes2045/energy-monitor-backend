package com.energymonitor.security.application.usecase;

import com.energymonitor.security.application.command.CheckPermissionQuery;
import com.energymonitor.security.application.exception.UserNotFoundException;
import com.energymonitor.security.application.port.in.CheckPermission;
import com.energymonitor.security.application.port.out.PermissionPersistencePort;
import com.energymonitor.security.application.port.out.SystemRolePermissionPersistencePort;
import com.energymonitor.security.application.port.out.SystemRolePersistencePort;
import com.energymonitor.security.application.port.out.UserPersistencePort;
import com.energymonitor.security.application.port.out.UserSystemRolePersistencePort;
import com.energymonitor.security.domain.model.Permission;
import com.energymonitor.security.domain.model.SystemRole;
import com.energymonitor.security.domain.model.User;
import com.energymonitor.security.domain.model.UserSystemRole;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authorizes a user against a permission code through the RBAC chain
 * {@code User → UserSystemRole → SystemRole → SystemRolePermission → Permission}.
 */
public class CheckPermissionService implements CheckPermission {

    private final UserPersistencePort userPort;
    private final PermissionPersistencePort permissionPort;
    private final SystemRolePersistencePort rolePort;
    private final UserSystemRolePersistencePort userSystemRolePort;
    private final SystemRolePermissionPersistencePort systemRolePermissionPort;

    public CheckPermissionService(UserPersistencePort userPort,
                                  PermissionPersistencePort permissionPort,
                                  SystemRolePersistencePort rolePort,
                                  UserSystemRolePersistencePort userSystemRolePort,
                                  SystemRolePermissionPersistencePort systemRolePermissionPort) {
        this.userPort = userPort;
        this.permissionPort = permissionPort;
        this.rolePort = rolePort;
        this.userSystemRolePort = userSystemRolePort;
        this.systemRolePermissionPort = systemRolePermissionPort;
    }

    @Override
    public boolean check(CheckPermissionQuery query) {
        User user = userPort.findActive(query.idUser())
                .orElseThrow(() -> new UserNotFoundException("no active user " + query.idUser()));
        if (!user.canAuthenticate()) {
            return false;
        }
        Permission permission = permissionPort.findActiveByCode(query.permissionCode()).orElse(null);
        if (permission == null) {
            return false;
        }
        Set<String> roleIds = userSystemRolePort.listActiveByUser(user.idUser()).stream()
                .map(UserSystemRole::idSystemRole)
                .filter(this::isAssignedRoleEnabled)
                .collect(Collectors.toSet());
        for (String roleId : roleIds) {
            boolean granted = systemRolePermissionPort.listActiveBySystemRole(roleId).stream()
                    .anyMatch(grant -> grant.idPermission().equals(permission.idPermission()));
            if (granted) {
                return true;
            }
        }
        return false;
    }

    private boolean isAssignedRoleEnabled(String idSystemRole) {
        return rolePort.findActive(idSystemRole)
                .filter(SystemRole::canBeAssigned)
                .isPresent();
    }
}