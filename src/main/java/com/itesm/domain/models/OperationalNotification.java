package com.itesm.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class OperationalNotification {
    private UUID id;
    private UUID recommendationId;
    private UUID hospitalId;
    private String audienceLabel;
    private String message;
    private String status;
    private UUID sentByUserId;
    private LocalDateTime sentAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getRecommendationId() { return recommendationId; }
    public void setRecommendationId(UUID recommendationId) { this.recommendationId = recommendationId; }
    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }
    public String getAudienceLabel() { return audienceLabel; }
    public void setAudienceLabel(String audienceLabel) { this.audienceLabel = audienceLabel; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UUID getSentByUserId() { return sentByUserId; }
    public void setSentByUserId(UUID sentByUserId) { this.sentByUserId = sentByUserId; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
