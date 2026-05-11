package com.itesm.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "operational_recommendation_audit")
public class OperationalRecommendationAuditEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private OperationalRecommendationEntity recommendation;

    @Column(name = "actor_user_id")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID actorUserId;

    @Column(name = "event_type", nullable = false, length = 32)
    private String eventType;

    @Column(name = "event_label", length = 255)
    private String eventLabel;

    @Column(name = "event_payload_json", columnDefinition = "TEXT")
    private String eventPayloadJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public OperationalRecommendationEntity getRecommendation() { return recommendation; }
    public void setRecommendation(OperationalRecommendationEntity recommendation) { this.recommendation = recommendation; }
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
