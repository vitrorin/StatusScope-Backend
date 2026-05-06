package com.itesm.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class DiagnosisEvaluationEventDto {
    private UUID id;
    private String diseaseName;
    private String diseaseCode;
    private String status;
    private LocalDateTime startedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getDiseaseName() { return diseaseName; }
    public void setDiseaseName(String diseaseName) { this.diseaseName = diseaseName; }

    public String getDiseaseCode() { return diseaseCode; }
    public void setDiseaseCode(String diseaseCode) { this.diseaseCode = diseaseCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
}
