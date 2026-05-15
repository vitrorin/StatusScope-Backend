package com.itesm.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hospital_staffing_profiles")
public class HospitalStaffingProfileEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private HospitalEntity hospital;

    @Column(name = "role_code", nullable = false, length = 32)
    private String roleCode;

    @Column(name = "role_name", nullable = false, length = 128)
    private String roleName;

    @Column(nullable = false)
    private int headcount;

    @Column(name = "on_shift_count", nullable = false)
    private int onShiftCount;

    @Column(name = "on_call_count", nullable = false)
    private int onCallCount;

    @Column(name = "standby_count", nullable = false)
    private int standbyCount;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public HospitalEntity getHospital() { return hospital; }
    public void setHospital(HospitalEntity hospital) { this.hospital = hospital; }
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
