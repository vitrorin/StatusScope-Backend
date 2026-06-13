package com.itesm.domain.models;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class User {
    private UUID id;
    private String fullName;
    private String email;
    private boolean active;
    private String externalAuthId;
    private UUID hospitalId;
    private UserStatus status;
    private LocalDateTime lastLoginAt;
    private String licenseNumber;
    private UUID specialtyId;
    private Set<Role> roles = new HashSet<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getExternalAuthId() {
        return externalAuthId;
    }

    public void setExternalAuthId(String externalAuthId) {
        this.externalAuthId = externalAuthId;
    }

    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public UUID getSpecialtyId() { return specialtyId; }
    public void setSpecialtyId(UUID specialtyId) { this.specialtyId = specialtyId; }

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}
