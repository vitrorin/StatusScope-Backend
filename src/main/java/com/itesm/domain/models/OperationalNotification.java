package com.itesm.domain.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OperationalNotification {
    private UUID id;
    private UUID recommendationId;
    private UUID hospitalId;
    private UUID audienceGroupId;
    private UUID audienceContactId;
    private String audienceType;
    private String audienceDepartmentCode;
    private String audienceLabel;
    private String message;
    private String status;
    private String deliveryChannel;
    private String deliveryStatusDetail;
    private String sourceActionCode;
    private UUID sentByUserId;
    private String language;
    private LocalDateTime sentAt;
    private List<OperationalNotificationRecipient> recipients = new ArrayList<>();

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
    public String getAudienceType() { return audienceType; }
    public void setAudienceType(String audienceType) { this.audienceType = audienceType; }
    public String getAudienceDepartmentCode() { return audienceDepartmentCode; }
    public void setAudienceDepartmentCode(String audienceDepartmentCode) { this.audienceDepartmentCode = audienceDepartmentCode; }
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
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public List<OperationalNotificationRecipient> getRecipients() { return recipients; }
    public void setRecipients(List<OperationalNotificationRecipient> recipients) { this.recipients = recipients; }
}
