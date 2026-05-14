package com.itesm.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "diagnosis_assistant_suggestions",
        indexes = {
                @Index(name = "idx_das_message", columnList = "message_id"),
                @Index(name = "idx_das_evaluation", columnList = "evaluation_id"),
                @Index(name = "idx_das_disease", columnList = "disease_id")
        }
)
public class DiagnosisAssistantSuggestionEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private DiagnosisAssistantMessageEntity message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private PatientEvaluationEntity evaluation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disease_id")
    private DiseaseEntity disease;

    @Column(name = "display_name", nullable = false, length = 256)
    private String displayName;

    @Column(name = "rank_order", nullable = false)
    private int rankOrder;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "rationale", columnDefinition = "TEXT")
    private String rationale;

    @Column(name = "locality_risk_level", length = 16)
    private String localityRiskLevel;

    @Column(name = "was_primary_suggestion", nullable = false)
    private boolean wasPrimarySuggestion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public DiagnosisAssistantMessageEntity getMessage() { return message; }
    public void setMessage(DiagnosisAssistantMessageEntity message) { this.message = message; }

    public PatientEvaluationEntity getEvaluation() { return evaluation; }
    public void setEvaluation(PatientEvaluationEntity evaluation) { this.evaluation = evaluation; }

    public DiseaseEntity getDisease() { return disease; }
    public void setDisease(DiseaseEntity disease) { this.disease = disease; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public int getRankOrder() { return rankOrder; }
    public void setRankOrder(int rankOrder) { this.rankOrder = rankOrder; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    public String getLocalityRiskLevel() { return localityRiskLevel; }
    public void setLocalityRiskLevel(String localityRiskLevel) { this.localityRiskLevel = localityRiskLevel; }

    public boolean isWasPrimarySuggestion() { return wasPrimarySuggestion; }
    public void setWasPrimarySuggestion(boolean wasPrimarySuggestion) { this.wasPrimarySuggestion = wasPrimarySuggestion; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
