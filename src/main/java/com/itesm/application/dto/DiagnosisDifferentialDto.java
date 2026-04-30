package com.itesm.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class DiagnosisDifferentialDto {
    private UUID id;
    private UUID diseaseId;
    private String diseaseCode;
    private String displayName;
    private BigDecimal confidence;
    private String rationale;
    private int rankOrder;
    private String localityRiskLevel;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getDiseaseId() { return diseaseId; }
    public void setDiseaseId(UUID diseaseId) { this.diseaseId = diseaseId; }

    public String getDiseaseCode() { return diseaseCode; }
    public void setDiseaseCode(String diseaseCode) { this.diseaseCode = diseaseCode; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    public int getRankOrder() { return rankOrder; }
    public void setRankOrder(int rankOrder) { this.rankOrder = rankOrder; }

    public String getLocalityRiskLevel() { return localityRiskLevel; }
    public void setLocalityRiskLevel(String localityRiskLevel) { this.localityRiskLevel = localityRiskLevel; }
}
