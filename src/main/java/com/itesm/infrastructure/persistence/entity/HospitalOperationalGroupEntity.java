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
@Table(name = "hospital_operational_groups")
public class HospitalOperationalGroupEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private HospitalEntity hospital;

    @Column(name = "group_code", nullable = false, length = 32)
    private String groupCode;

    @Column(name = "group_name", nullable = false, length = 128)
    private String groupName;

    @Column(name = "group_type", nullable = false, length = 32)
    private String groupType;

    @Column(name = "department_code", length = 32)
    private String departmentCode;

    @Column(name = "is_assignable", nullable = false)
    private boolean assignable;

    @Column(name = "is_notifiable", nullable = false)
    private boolean notifiable;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public HospitalEntity getHospital() { return hospital; }
    public void setHospital(HospitalEntity hospital) { this.hospital = hospital; }
    public String getGroupCode() { return groupCode; }
    public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getGroupType() { return groupType; }
    public void setGroupType(String groupType) { this.groupType = groupType; }
    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
    public boolean isAssignable() { return assignable; }
    public void setAssignable(boolean assignable) { this.assignable = assignable; }
    public boolean isNotifiable() { return notifiable; }
    public void setNotifiable(boolean notifiable) { this.notifiable = notifiable; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
