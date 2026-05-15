package com.itesm.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class HospitalStaffingProfile {
    private UUID id;
    private UUID hospitalId;
    private String roleCode;
    private String roleName;
    private int headcount;
    private int onShiftCount;
    private int onCallCount;
    private int standbyCount;
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public int getHeadcount() { return headcount; }
    public void setHeadcount(int headcount) { this.headcount = headcount; }
    public int getOnShiftCount() { return onShiftCount; }
    public void setOnShiftCount(int onShiftCount) { this.onShiftCount = onShiftCount; }
    public int getOnCallCount() { return onCallCount; }
    public void setOnCallCount(int onCallCount) { this.onCallCount = onCallCount; }
    public int getStandbyCount() { return standbyCount; }
    public void setStandbyCount(int standbyCount) { this.standbyCount = standbyCount; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
