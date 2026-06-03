package com.itesm.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class OperationalContactUpsertDto {
    @NotBlank
    private String displayName;
    @NotBlank
    private String roleLabel;
    private String departmentCode;
    @NotBlank
    @Email
    private String email;
    private boolean assignable;
    private boolean notifiable;
    private String availabilityStatus;

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getRoleLabel() { return roleLabel; }
    public void setRoleLabel(String roleLabel) { this.roleLabel = roleLabel; }
    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isAssignable() { return assignable; }
    public void setAssignable(boolean assignable) { this.assignable = assignable; }
    public boolean isNotifiable() { return notifiable; }
    public void setNotifiable(boolean notifiable) { this.notifiable = notifiable; }
    public String getAvailabilityStatus() { return availabilityStatus; }
    public void setAvailabilityStatus(String availabilityStatus) { this.availabilityStatus = availabilityStatus; }
}
