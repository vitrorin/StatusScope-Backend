package com.itesm.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class OperationalNotification {
    private UUID id;
    private UUID recommendationId;
    private UUID hospitalId;
    private UUID audienceGroupId;
    private UUID audienceContactId;
    private String audienceLabel;
    private String message;
    private String status;
    private String deliveryChannel;
    private String deliveryStatusDetail;
    private String sourceActionCode;
    private UUID sentByUserId;
    private LocalDateTime sentAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getRecommendationId() { return recommendationId; }
    public void setRecommendationId(UUID recommendationId) { this.recommendationId = recommendationId; }
    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }
    public UUID getAudienceGroupId() { return audienceGroupId; }
    public void setAudienceGroupId(UUID audienceGroupId) { this.audienceGroupId = audienceGroupId; }
    public UUID getAudienceContactId() { return audienceContactId; }
    public void setAudienceContactId(UUID audienceContactId) { this.audienceContactId = audienceContactId; }
    public String getAudienceLabel() { return audienceLabel; }
    public void setAudienceLabel(String audienceLabel) { this.audienceLabel = audienceLabel; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDeliveryChannel() { return deliveryChannel; }
    public void setDeliveryChannel(String deliveryChannel) { this.deliveryChannel = deliveryChannel; }
    public String getDeliveryStatusDetail() { return deliveryStatusDetail; }
    public void setDeliveryStatusDetail(String deliveryStatusDetail) { this.deliveryStatusDetail = deliveryStatusDetail; }
    public String getSourceActionCode() { return sourceActionCode; }
    public void setSourceActionCode(String sourceActionCode) { this.sourceActionCode = sourceActionCode; }
    public UUID getSentByUserId() { return sentByUserId; }
    public void setSentByUserId(UUID sentByUserId) { this.sentByUserId = sentByUserId; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
