package com.itesm.application.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public class AssistantFeedbackDto {

    @NotBlank
    private String finalDecisionSource;

    private UUID finalDiseaseId;

    private String finalDiagnosisLabel;

    private String doctorFeedbackNotes;

    private UUID acceptedAssistantMessageId;

    public String getFinalDecisionSource() { return finalDecisionSource; }
    public void setFinalDecisionSource(String finalDecisionSource) { this.finalDecisionSource = finalDecisionSource; }

    public UUID getFinalDiseaseId() { return finalDiseaseId; }
    public void setFinalDiseaseId(UUID finalDiseaseId) { this.finalDiseaseId = finalDiseaseId; }

    public String getFinalDiagnosisLabel() { return finalDiagnosisLabel; }
    public void setFinalDiagnosisLabel(String finalDiagnosisLabel) { this.finalDiagnosisLabel = finalDiagnosisLabel; }

    public String getDoctorFeedbackNotes() { return doctorFeedbackNotes; }
    public void setDoctorFeedbackNotes(String doctorFeedbackNotes) { this.doctorFeedbackNotes = doctorFeedbackNotes; }

    public UUID getAcceptedAssistantMessageId() { return acceptedAssistantMessageId; }
    public void setAcceptedAssistantMessageId(UUID acceptedAssistantMessageId) { this.acceptedAssistantMessageId = acceptedAssistantMessageId; }
}
