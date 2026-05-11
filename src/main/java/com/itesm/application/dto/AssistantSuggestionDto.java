package com.itesm.application.dto;

import java.util.UUID;

public class AssistantSuggestionDto {
    private UUID id;
    private UUID messageId;
    private UUID diseaseId;
    private String displayName;
    private int rankOrder;
    private Double confidence;
    private String rationale;
    private String localityRiskLevel;
    private boolean primary;

    public AssistantSuggestionDto() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getMessageId() { return messageId; }
    public void setMessageId(UUID messageId) { this.messageId = messageId; }

    public UUID getDiseaseId() { return diseaseId; }
    public void setDiseaseId(UUID diseaseId) { this.diseaseId = diseaseId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public int getRankOrder() { return rankOrder; }
    public void setRankOrder(int rankOrder) { this.rankOrder = rankOrder; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    public String getLocalityRiskLevel() { return localityRiskLevel; }
    public void setLocalityRiskLevel(String localityRiskLevel) { this.localityRiskLevel = localityRiskLevel; }

    public boolean isPrimary() { return primary; }
    public void setPrimary(boolean primary) { this.primary = primary; }
}
