package com.itesm.application.dto;

import java.time.LocalDateTime;

public class OutbreakSummaryDto {
    private String diseaseName;
    private String municipalityName;
    private String cityName;
    private String stateName;
    private int caseCount;
    private LocalDateTime startedAt;

    public OutbreakSummaryDto() {}

    public OutbreakSummaryDto(String diseaseName, int caseCount, LocalDateTime startedAt) {
        this.diseaseName = diseaseName;
        this.caseCount = caseCount;
        this.startedAt = startedAt;
    }

    public OutbreakSummaryDto(String diseaseName, String municipalityName, String cityName, String stateName, int caseCount, LocalDateTime startedAt) {
        this.diseaseName = diseaseName;
        this.municipalityName = municipalityName;
        this.cityName = cityName;
        this.stateName = stateName;
        this.caseCount = caseCount;
        this.startedAt = startedAt;
    }

    public String getDiseaseName() { return diseaseName; }
    public void setDiseaseName(String diseaseName) { this.diseaseName = diseaseName; }

    public String getMunicipalityName() { return municipalityName; }
    public void setMunicipalityName(String municipalityName) { this.municipalityName = municipalityName; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public String getStateName() { return stateName; }
    public void setStateName(String stateName) { this.stateName = stateName; }

    public int getCaseCount() { return caseCount; }
    public void setCaseCount(int caseCount) { this.caseCount = caseCount; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
}
