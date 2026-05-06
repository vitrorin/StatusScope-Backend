package com.itesm.application.dto;

import java.time.LocalDateTime;

public class OutbreakSummaryDto {
    private String diseaseName;
    private String scope;
    private String municipalityName;
    private String stateName;
    private int caseCount;
    private String confirmationStatus;
    private LocalDateTime startedAt;

    public OutbreakSummaryDto() {}

    public OutbreakSummaryDto(String diseaseName, int caseCount, LocalDateTime startedAt) {
        this.diseaseName = diseaseName;
        this.caseCount = caseCount;
        this.startedAt = startedAt;
    }

    public OutbreakSummaryDto(String diseaseName, String scope, String municipalityName, String stateName, int caseCount, String confirmationStatus, LocalDateTime startedAt) {
        this.diseaseName = diseaseName;
        this.scope = scope;
        this.municipalityName = municipalityName;
        this.stateName = stateName;
        this.caseCount = caseCount;
        this.confirmationStatus = confirmationStatus;
        this.startedAt = startedAt;
    }

    public String getDiseaseName() { return diseaseName; }
    public void setDiseaseName(String diseaseName) { this.diseaseName = diseaseName; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getMunicipalityName() { return municipalityName; }
    public void setMunicipalityName(String municipalityName) { this.municipalityName = municipalityName; }

    public String getStateName() { return stateName; }
    public void setStateName(String stateName) { this.stateName = stateName; }

    public int getCaseCount() { return caseCount; }
    public void setCaseCount(int caseCount) { this.caseCount = caseCount; }

    public String getConfirmationStatus() { return confirmationStatus; }
    public void setConfirmationStatus(String confirmationStatus) { this.confirmationStatus = confirmationStatus; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
}
