package com.itesm.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class OperationalRecommendation {
    private UUID id;
    private UUID hospitalId;
    private UUID sourceAlertId;
    private UUID sourceOutbreakId;
    private String type;
    private String severity;
    private String status;
    private String category;
    private String title;
    private String description;
    private String expectedImpact;
    private String urgencyWindow;
    private BigDecimal confidenceScore;
    private String imageMode;
    private String rationaleJson;
    private String recommendedActionsJson;
    private String affectedDepartmentsJson;
    private String affectedResourcesJson;
    private String modelProvider;
    private String modelVersion;
    private String inputContextJson;
    private String createdByMode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }
    public UUID getSourceAlertId() { return sourceAlertId; }
    public void setSourceAlertId(UUID sourceAlertId) { this.sourceAlertId = sourceAlertId; }
    public UUID getSourceOutbreakId() { return sourceOutbreakId; }
    public void setSourceOutbreakId(UUID sourceOutbreakId) { this.sourceOutbreakId = sourceOutbreakId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getExpectedImpact() { return expectedImpact; }
    public void setExpectedImpact(String expectedImpact) { this.expectedImpact = expectedImpact; }
    public String getUrgencyWindow() { return urgencyWindow; }
    public void setUrgencyWindow(String urgencyWindow) { this.urgencyWindow = urgencyWindow; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    public String getImageMode() { return imageMode; }
    public void setImageMode(String imageMode) { this.imageMode = imageMode; }
    public String getRationaleJson() { return rationaleJson; }
    public void setRationaleJson(String rationaleJson) { this.rationaleJson = rationaleJson; }
    public String getRecommendedActionsJson() { return recommendedActionsJson; }
    public void setRecommendedActionsJson(String recommendedActionsJson) { this.recommendedActionsJson = recommendedActionsJson; }
    public String getAffectedDepartmentsJson() { return affectedDepartmentsJson; }
    public void setAffectedDepartmentsJson(String affectedDepartmentsJson) { this.affectedDepartmentsJson = affectedDepartmentsJson; }
    public String getAffectedResourcesJson() { return affectedResourcesJson; }
    public void setAffectedResourcesJson(String affectedResourcesJson) { this.affectedResourcesJson = affectedResourcesJson; }
    public String getModelProvider() { return modelProvider; }
    public void setModelProvider(String modelProvider) { this.modelProvider = modelProvider; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public String getInputContextJson() { return inputContextJson; }
    public void setInputContextJson(String inputContextJson) { this.inputContextJson = inputContextJson; }
    public String getCreatedByMode() { return createdByMode; }
    public void setCreatedByMode(String createdByMode) { this.createdByMode = createdByMode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
