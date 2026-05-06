package com.itesm.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbreaks")
public class OutbreakEntity {

    public static final String SCOPE_MUNICIPALITY = "MUNICIPALITY";
    public static final String SCOPE_STATE = "STATE";
    public static final String CONFIRMATION_SUSPECTED = "SUSPECTED";
    public static final String CONFIRMATION_CONFIRMED = "CONFIRMED";

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "disease_id", nullable = false)
    private DiseaseEntity disease;

    @Column(nullable = false, length = 16)
    private String scope;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "municipality_id")
    private MunicipalityEntity municipality;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "state_id")
    private StateEntity state;

    @Column(name = "case_count", nullable = false)
    private int caseCount;

    @Column(name = "confirmation_status", nullable = false, length = 16)
    private String confirmationStatus;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public DiseaseEntity getDisease() { return disease; }
    public void setDisease(DiseaseEntity disease) { this.disease = disease; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public MunicipalityEntity getMunicipality() { return municipality; }
    public void setMunicipality(MunicipalityEntity municipality) { this.municipality = municipality; }

    public StateEntity getState() { return state; }
    public void setState(StateEntity state) { this.state = state; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    @PreUpdate
    void validateScopeLocation() {
        if (!CONFIRMATION_SUSPECTED.equals(confirmationStatus) && !CONFIRMATION_CONFIRMED.equals(confirmationStatus)) {
            throw new IllegalStateException("Unsupported outbreak confirmation status: " + confirmationStatus);
        }
        if (SCOPE_MUNICIPALITY.equals(scope)) {
            if (municipality == null || state != null) {
                throw new IllegalStateException("Municipality outbreaks require municipality_id and no state_id");
            }
            return;
        }
        if (SCOPE_STATE.equals(scope)) {
            if (state == null || municipality != null) {
                throw new IllegalStateException("State outbreaks require state_id and no municipality_id");
            }
            return;
        }
        throw new IllegalStateException("Unsupported outbreak scope: " + scope);
    }
}
