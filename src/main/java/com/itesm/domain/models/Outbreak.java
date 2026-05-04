package com.itesm.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class Outbreak {
    private UUID id;
    private Disease disease;
    private String scope;
    private State state;
    private Municipality municipality;
    private int caseCount;
    private String confirmationStatus;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Disease getDisease() { return disease; }
    public void setDisease(Disease disease) { this.disease = disease; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public State getState() { return state; }
    public void setState(State state) { this.state = state; }

    public Municipality getMunicipality() { return municipality; }
    public void setMunicipality(Municipality municipality) { this.municipality = municipality; }

    public int getCaseCount() { return caseCount; }
    public void setCaseCount(int caseCount) { this.caseCount = caseCount; }

    public String getConfirmationStatus() { return confirmationStatus; }
    public void setConfirmationStatus(String confirmationStatus) { this.confirmationStatus = confirmationStatus; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
}
