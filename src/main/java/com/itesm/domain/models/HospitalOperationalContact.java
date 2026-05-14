package com.itesm.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class HospitalOperationalContact {
    private UUID id;
    private UUID hospitalId;
    private UUID userId;
    private String displayName;
    private String roleLabel;
    private String departmentCode;
    private String contactChannel;
    private String contactValue;
    private String availabilityStatus;
    private boolean assignable;
    private boolean notifiable;
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getRoleLabel() { return roleLabel; }
    public void setRoleLabel(String roleLabel) { this.roleLabel = roleLabel; }
    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
    public String getContactChannel() { return contactChannel; }
    public void setContactChannel(String contactChannel) { this.contactChannel = contactChannel; }
    public String getContactValue() { return contactValue; }
    public void setContactValue(String contactValue) { this.contactValue = contactValue; }
    public String getAvailabilityStatus() { return availabilityStatus; }
    public void setAvailabilityStatus(String availabilityStatus) { this.availabilityStatus = availabilityStatus; }
    public boolean isAssignable() { return assignable; }
    public void setAssignable(boolean assignable) { this.assignable = assignable; }
    public boolean isNotifiable() { return notifiable; }
    public void setNotifiable(boolean notifiable) { this.notifiable = notifiable; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
