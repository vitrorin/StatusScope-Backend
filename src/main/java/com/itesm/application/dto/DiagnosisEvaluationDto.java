package com.itesm.application.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DiagnosisEvaluationDto {
    private UUID id;
    private String status;
    private String symptomsText;
    private String clinicalNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime finalizedAt;
    private UUID finalDiseaseId;
    private String finalDiseaseName;
    private String finalDiagnosisLabel;
    private String finalDecisionSource;
    private String doctorFeedbackNotes;
    private DiagnosisEvaluationPatientDto patient;
    private DiagnosisEvaluationEventDto event;
    private List<DiagnosisDifferentialDto> differentialDiagnoses = new ArrayList<>();
    private List<DiagnosisRecommendedTestDto> recommendedTests = new ArrayList<>();
    private List<DiagnosisEvaluationFileDto> files = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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

    public UUID getFinalDiseaseId() { return finalDiseaseId; }
    public void setFinalDiseaseId(UUID finalDiseaseId) { this.finalDiseaseId = finalDiseaseId; }

    public String getFinalDiseaseName() { return finalDiseaseName; }
    public void setFinalDiseaseName(String finalDiseaseName) { this.finalDiseaseName = finalDiseaseName; }

    public String getFinalDiagnosisLabel() { return finalDiagnosisLabel; }
    public void setFinalDiagnosisLabel(String finalDiagnosisLabel) { this.finalDiagnosisLabel = finalDiagnosisLabel; }

    public String getFinalDecisionSource() { return finalDecisionSource; }
    public void setFinalDecisionSource(String finalDecisionSource) { this.finalDecisionSource = finalDecisionSource; }

    public String getDoctorFeedbackNotes() { return doctorFeedbackNotes; }
    public void setDoctorFeedbackNotes(String doctorFeedbackNotes) { this.doctorFeedbackNotes = doctorFeedbackNotes; }

    public DiagnosisEvaluationPatientDto getPatient() { return patient; }
    public void setPatient(DiagnosisEvaluationPatientDto patient) { this.patient = patient; }

    public DiagnosisEvaluationEventDto getEvent() { return event; }
    public void setEvent(DiagnosisEvaluationEventDto event) { this.event = event; }

    public List<DiagnosisDifferentialDto> getDifferentialDiagnoses() { return differentialDiagnoses; }
    public void setDifferentialDiagnoses(List<DiagnosisDifferentialDto> differentialDiagnoses) { this.differentialDiagnoses = differentialDiagnoses; }

    public List<DiagnosisRecommendedTestDto> getRecommendedTests() { return recommendedTests; }
    public void setRecommendedTests(List<DiagnosisRecommendedTestDto> recommendedTests) { this.recommendedTests = recommendedTests; }

    public List<DiagnosisEvaluationFileDto> getFiles() { return files; }
    public void setFiles(List<DiagnosisEvaluationFileDto> files) { this.files = files; }
}
