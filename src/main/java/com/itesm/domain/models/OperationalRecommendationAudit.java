package com.itesm.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class OperationalRecommendationAudit {
    private UUID id;
    private UUID recommendationId;
    private UUID actorUserId;
    private String eventType;
    private String eventLabel;
    private String eventPayloadJson;
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getRecommendationId() { return recommendationId; }
    public void setRecommendationId(UUID recommendationId) { this.recommendationId = recommendationId; }
    public UUID getActorUserId() { return actorUserId; }
    public void setActorUserId(UUID actorUserId) { this.actorUserId = actorUserId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getEventLabel() { return eventLabel; }
    public void setEventLabel(String eventLabel) { this.eventLabel = eventLabel; }
    public String getEventPayloadJson() { return eventPayloadJson; }
    public void setEventPayloadJson(String eventPayloadJson) { this.eventPayloadJson = eventPayloadJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
