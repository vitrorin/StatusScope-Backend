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
        name = "diagnosis_assistant_retrieved_cases",
        indexes = {
                @Index(name = "idx_darc_message", columnList = "message_id"),
                @Index(name = "idx_darc_retrieved", columnList = "retrieved_evaluation_id")
        }
)
public class DiagnosisAssistantRetrievedCaseEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private DiagnosisAssistantMessageEntity message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retrieved_evaluation_id", nullable = false)
    private PatientEvaluationEntity retrievedEvaluation;

    @Column(name = "rank_order", nullable = false)
    private int rankOrder;

    @Column(name = "similarity_score")
    private Double similarityScore;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public DiagnosisAssistantMessageEntity getMessage() { return message; }
    public void setMessage(DiagnosisAssistantMessageEntity message) { this.message = message; }

    public PatientEvaluationEntity getRetrievedEvaluation() { return retrievedEvaluation; }
    public void setRetrievedEvaluation(PatientEvaluationEntity retrievedEvaluation) {
        this.retrievedEvaluation = retrievedEvaluation;
    }

    public int getRankOrder() { return rankOrder; }
    public void setRankOrder(int rankOrder) { this.rankOrder = rankOrder; }

    public Double getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(Double similarityScore) { this.similarityScore = similarityScore; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
