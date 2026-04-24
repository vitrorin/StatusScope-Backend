package com.itesm.application.dto;

import java.util.Set;
import java.util.UUID;

public class UserSummaryDto {
    private UUID id;
    private String email;
    private String fullName;
    private UUID hospitalId;
    private String hospitalName;
    private Set<String> roles;
    private Set<String> privileges;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }

    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }

    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }

    public Set<String> getPrivileges() { return privileges; }
    public void setPrivileges(Set<String> privileges) { this.privileges = privileges; }
}
