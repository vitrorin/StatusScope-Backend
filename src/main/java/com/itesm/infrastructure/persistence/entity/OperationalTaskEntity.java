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
@Table(name = "operational_tasks")
public class OperationalTaskEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private OperationalRecommendationEntity recommendation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private HospitalEntity hospital;

    @Column(name = "owner_user_id")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID ownerUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_contact_id")
    private HospitalOperationalContactEntity ownerContact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_group_id")
    private HospitalOperationalGroupEntity ownerGroup;

    @Column(name = "owner_label", length = 255)
    private String ownerLabel;

    @Column(name = "department_label", length = 255)
    private String departmentLabel;

    @Column(name = "deadline_at")
    private LocalDateTime deadlineAt;

    @Column(nullable = false, length = 16)
    private String priority;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "source_action_code", length = 32)
    private String sourceActionCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_by_recommendation_id")
    private OperationalRecommendationEntity recommendedByRecommendation;

    @Column(name = "created_by_user_id")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public OperationalRecommendationEntity getRecommendation() { return recommendation; }
    public void setRecommendation(OperationalRecommendationEntity recommendation) { this.recommendation = recommendation; }
    public HospitalEntity getHospital() { return hospital; }
    public void setHospital(HospitalEntity hospital) { this.hospital = hospital; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(UUID ownerUserId) { this.ownerUserId = ownerUserId; }
    public HospitalOperationalContactEntity getOwnerContact() { return ownerContact; }
    public void setOwnerContact(HospitalOperationalContactEntity ownerContact) { this.ownerContact = ownerContact; }
    public HospitalOperationalGroupEntity getOwnerGroup() { return ownerGroup; }
    public void setOwnerGroup(HospitalOperationalGroupEntity ownerGroup) { this.ownerGroup = ownerGroup; }
    public String getOwnerLabel() { return ownerLabel; }
    public void setOwnerLabel(String ownerLabel) { this.ownerLabel = ownerLabel; }
    public String getDepartmentLabel() { return departmentLabel; }
    public void setDepartmentLabel(String departmentLabel) { this.departmentLabel = departmentLabel; }
    public LocalDateTime getDeadlineAt() { return deadlineAt; }
    public void setDeadlineAt(LocalDateTime deadlineAt) { this.deadlineAt = deadlineAt; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSourceActionCode() { return sourceActionCode; }
    public void setSourceActionCode(String sourceActionCode) { this.sourceActionCode = sourceActionCode; }
    public OperationalRecommendationEntity getRecommendedByRecommendation() { return recommendedByRecommendation; }
    public void setRecommendedByRecommendation(OperationalRecommendationEntity recommendedByRecommendation) { this.recommendedByRecommendation = recommendedByRecommendation; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID createdByUserId) { this.createdByUserId = createdByUserId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
