package com.itesm.application.security;

import java.util.Set;
import java.util.UUID;

public class CurrentUser {
    private final UUID userId;
    private final String externalAuthId;
    private final String email;
    private final String fullName;
    private final UUID hospitalId;
    private final Set<String> roles;
    private final Set<String> privileges;

    public CurrentUser(UUID userId, String externalAuthId, String email, String fullName,
                       UUID hospitalId, Set<String> roles, Set<String> privileges) {
        this.userId = userId;
        this.externalAuthId = externalAuthId;
        this.email = email;
        this.fullName = fullName;
        this.hospitalId = hospitalId;
        this.roles = roles;
        this.privileges = privileges;
    }

    public boolean hasRole(String roleCode) {
        return roles.contains(roleCode);
    }

    public boolean hasPrivilege(String privilegeCode) {
        return privileges.contains(privilegeCode);
    }

    public boolean isSystemAdmin() {
        return roles.contains("SYSTEM_ADMIN");
    }

    public UUID getUserId() { return userId; }
    public String getExternalAuthId() { return externalAuthId; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public UUID getHospitalId() { return hospitalId; }
    public Set<String> getRoles() { return roles; }
    public Set<String> getPrivileges() { return privileges; }
}

