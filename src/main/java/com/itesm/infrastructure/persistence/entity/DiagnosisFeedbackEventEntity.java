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
        name = "diagnosis_feedback_events",
        indexes = {
                @Index(name = "idx_dfe_evaluation", columnList = "evaluation_id"),
                @Index(name = "idx_dfe_thread", columnList = "thread_id"),
                @Index(name = "idx_dfe_doctor", columnList = "doctor_user_id"),
                @Index(name = "idx_dfe_hospital", columnList = "hospital_id"),
                @Index(name = "idx_dfe_type", columnList = "feedback_type")
        }
)
public class DiagnosisFeedbackEventEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private PatientEvaluationEntity evaluation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id")
    private DiagnosisAssistantThreadEntity thread;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_user_id", nullable = false)
    private UserEntity doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id")
    private HospitalEntity hospital;

    @Column(name = "feedback_type", nullable = false, length = 48)
    private String feedbackType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_assistant_message_id")
    private DiagnosisAssistantMessageEntity acceptedAssistantMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "final_disease_id")
    private DiseaseEntity finalDisease;

    @Column(name = "final_diagnosis_label", length = 256)
    private String finalDiagnosisLabel;

    @Column(name = "feedback_notes", columnDefinition = "TEXT")
    private String feedbackNotes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PatientEvaluationEntity getEvaluation() { return evaluation; }
    public void setEvaluation(PatientEvaluationEntity evaluation) { this.evaluation = evaluation; }

    public DiagnosisAssistantThreadEntity getThread() { return thread; }
    public void setThread(DiagnosisAssistantThreadEntity thread) { this.thread = thread; }

    public UserEntity getDoctor() { return doctor; }
    public void setDoctor(UserEntity doctor) { this.doctor = doctor; }

    public HospitalEntity getHospital() { return hospital; }
    public void setHospital(HospitalEntity hospital) { this.hospital = hospital; }

    public String getFeedbackType() { return feedbackType; }
    public void setFeedbackType(String feedbackType) { this.feedbackType = feedbackType; }

    public DiagnosisAssistantMessageEntity getAcceptedAssistantMessage() { return acceptedAssistantMessage; }
    public void setAcceptedAssistantMessage(DiagnosisAssistantMessageEntity acceptedAssistantMessage) {
        this.acceptedAssistantMessage = acceptedAssistantMessage;
    }

    public DiseaseEntity getFinalDisease() { return finalDisease; }
    public void setFinalDisease(DiseaseEntity finalDisease) { this.finalDisease = finalDisease; }

    public String getFinalDiagnosisLabel() { return finalDiagnosisLabel; }
    public void setFinalDiagnosisLabel(String finalDiagnosisLabel) { this.finalDiagnosisLabel = finalDiagnosisLabel; }

    public String getFeedbackNotes() { return feedbackNotes; }
    public void setFeedbackNotes(String feedbackNotes) { this.feedbackNotes = feedbackNotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
