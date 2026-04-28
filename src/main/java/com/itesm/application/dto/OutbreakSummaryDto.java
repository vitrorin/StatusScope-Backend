package com.itesm.application.dto;

import java.time.LocalDateTime;

public class OutbreakSummaryDto {
    private String diseaseName;
    private int caseCount;
    private LocalDateTime startedAt;

    public OutbreakSummaryDto() {}

    public OutbreakSummaryDto(String diseaseName, int caseCount, LocalDateTime startedAt) {
        this.diseaseName = diseaseName;
        this.caseCount = caseCount;
        this.startedAt = startedAt;
    }

    public String getDiseaseName() { return diseaseName; }
    public void setDiseaseName(String diseaseName) { this.diseaseName = diseaseName; }

    public int getCaseCount() { return caseCount; }
    public void setCaseCount(int caseCount) { this.caseCount = caseCount; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
}
