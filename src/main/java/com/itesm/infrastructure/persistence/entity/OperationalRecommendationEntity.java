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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "operational_recommendations")
public class OperationalRecommendationEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private HospitalEntity hospital;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_alert_id")
    private AlertEntity sourceAlert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_outbreak_id")
    private OutbreakEntity sourceOutbreak;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false, length = 16)
    private String severity;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(length = 64)
    private String category;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "expected_impact", length = 512)
    private String expectedImpact;

    @Column(name = "urgency_window", length = 128)
    private String urgencyWindow;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "image_mode", length = 32)
    private String imageMode;

    @Column(name = "rationale_json", columnDefinition = "TEXT")
    private String rationaleJson;

    @Column(name = "recommended_actions_json", columnDefinition = "TEXT")
    private String recommendedActionsJson;

    @Column(name = "affected_departments_json", columnDefinition = "TEXT")
    private String affectedDepartmentsJson;

    @Column(name = "affected_resources_json", columnDefinition = "TEXT")
    private String affectedResourcesJson;

    @Column(name = "model_provider", length = 64)
    private String modelProvider;

    @Column(name = "model_version", length = 64)
    private String modelVersion;

    @Column(name = "input_context_json", columnDefinition = "TEXT")
    private String inputContextJson;

    @Column(name = "created_by_mode", nullable = false, length = 32)
    private String createdByMode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public HospitalEntity getHospital() { return hospital; }
    public void setHospital(HospitalEntity hospital) { this.hospital = hospital; }
    public AlertEntity getSourceAlert() { return sourceAlert; }
    public void setSourceAlert(AlertEntity sourceAlert) { this.sourceAlert = sourceAlert; }
    public OutbreakEntity getSourceOutbreak() { return sourceOutbreak; }
    public void setSourceOutbreak(OutbreakEntity sourceOutbreak) { this.sourceOutbreak = sourceOutbreak; }
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
