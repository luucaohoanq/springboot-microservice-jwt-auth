package com.lcaohoanq.commonlibrary.utils;

import com.lcaohoanq.commonlibrary.enums.Role;

import java.util.Map;
import java.util.Set;

public class PermissionUtils {

    private static final Map<Role, Set<Role>> PERMISSIONS = Map.of(
            Role.USER, Set.of(Role.USER),
            Role.STAFF, Set.of(Role.USER, Role.STAFF),
            Role.ADMIN, Set.of(Role.USER, Role.STAFF, Role.ADMIN)
    );

    public static boolean hasPermission(String userRole, Role requiredRole) {
        try {
            Role current = Role.valueOf(userRole);
            return PERMISSIONS.getOrDefault(current, Set.of()).contains(requiredRole);
        } catch (Exception e) {
            return false;
        }
    }

}
