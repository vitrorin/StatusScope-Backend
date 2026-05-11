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
        name = "patient_evaluations",
        indexes = {
                @Index(name = "idx_pe_patient", columnList = "patient_id"),
                @Index(name = "idx_pe_doctor", columnList = "doctor_user_id"),
                @Index(name = "idx_pe_event", columnList = "event_id"),
                @Index(name = "idx_pe_status", columnList = "status")
        }
)
public class PatientEvaluationEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_user_id", nullable = false)
    private UserEntity doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private EventEntity event;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "symptoms_text", nullable = false, columnDefinition = "TEXT")
    private String symptomsText;

    @Column(name = "clinical_notes", columnDefinition = "TEXT")
    private String clinicalNotes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "final_disease_id")
    private DiseaseEntity finalDisease;

    @Column(name = "final_diagnosis_label", length = 256)
    private String finalDiagnosisLabel;

    @Column(name = "final_decision_source", length = 48)
    private String finalDecisionSource;

    @Column(name = "doctor_feedback_notes", columnDefinition = "TEXT")
    private String doctorFeedbackNotes;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PatientEntity getPatient() { return patient; }
    public void setPatient(PatientEntity patient) { this.patient = patient; }

    public UserEntity getDoctor() { return doctor; }
    public void setDoctor(UserEntity doctor) { this.doctor = doctor; }

    public EventEntity getEvent() { return event; }
    public void setEvent(EventEntity event) { this.event = event; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSymptomsText() { return symptomsText; }
    public void setSymptomsText(String symptomsText) { this.symptomsText = symptomsText; }

    public String getClinicalNotes() { return clinicalNotes; }
    public void setClinicalNotes(String clinicalNotes) { this.clinicalNotes = clinicalNotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getFinalizedAt() { return finalizedAt; }
    public void setFinalizedAt(LocalDateTime finalizedAt) { this.finalizedAt = finalizedAt; }

    public DiseaseEntity getFinalDisease() { return finalDisease; }
    public void setFinalDisease(DiseaseEntity finalDisease) { this.finalDisease = finalDisease; }

    public String getFinalDiagnosisLabel() { return finalDiagnosisLabel; }
    public void setFinalDiagnosisLabel(String finalDiagnosisLabel) { this.finalDiagnosisLabel = finalDiagnosisLabel; }

    public String getFinalDecisionSource() { return finalDecisionSource; }
    public void setFinalDecisionSource(String finalDecisionSource) { this.finalDecisionSource = finalDecisionSource; }

    public String getDoctorFeedbackNotes() { return doctorFeedbackNotes; }
    public void setDoctorFeedbackNotes(String doctorFeedbackNotes) { this.doctorFeedbackNotes = doctorFeedbackNotes; }
}
