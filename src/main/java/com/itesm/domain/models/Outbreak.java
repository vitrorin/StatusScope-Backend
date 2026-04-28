package com.itesm.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class Outbreak {
    private UUID id;
    private Disease disease;
    private Region region;
    private int caseCount;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Disease getDisease() { return disease; }
    public void setDisease(Disease disease) { this.disease = disease; }

    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }

    public int getCaseCount() { return caseCount; }
    public void setCaseCount(int caseCount) { this.caseCount = caseCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
}
