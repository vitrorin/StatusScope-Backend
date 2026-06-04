package com.itesm.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class OperationalTask {
    private UUID id;
    private UUID recommendationId;
    private UUID hospitalId;
    private UUID ownerUserId;
    private UUID ownerContactId;
    private UUID ownerGroupId;
    private String ownerLabel;
    private String departmentLabel;
    private LocalDateTime deadlineAt;
    private String priority;
    private String notes;
    private String status;
    private String sourceActionCode;
    private UUID recommendedByRecommendationId;
    private UUID createdByUserId;
    private String language;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getRecommendationId() { return recommendationId; }
    public void setRecommendationId(UUID recommendationId) { this.recommendationId = recommendationId; }
    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(UUID ownerUserId) { this.ownerUserId = ownerUserId; }
    public UUID getOwnerContactId() { return ownerContactId; }
    public void setOwnerContactId(UUID ownerContactId) { this.ownerContactId = ownerContactId; }
    public UUID getOwnerGroupId() { return ownerGroupId; }
    public void setOwnerGroupId(UUID ownerGroupId) { this.ownerGroupId = ownerGroupId; }
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
    public UUID getRecommendedByRecommendationId() { return recommendedByRecommendationId; }
    public void setRecommendedByRecommendationId(UUID recommendedByRecommendationId) { this.recommendedByRecommendationId = recommendedByRecommendationId; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID createdByUserId) { this.createdByUserId = createdByUserId; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
