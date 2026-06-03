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
@Table(name = "operational_notifications")
public class OperationalNotificationEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private OperationalRecommendationEntity recommendation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private HospitalEntity hospital;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audience_group_id")
    private HospitalOperationalGroupEntity audienceGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audience_contact_id")
    private HospitalOperationalContactEntity audienceContact;

    @Column(name = "audience_type", length = 32)
    private String audienceType;

    @Column(name = "audience_department_code", length = 32)
    private String audienceDepartmentCode;

    @Column(name = "audience_label", length = 255)
    private String audienceLabel;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "delivery_channel", length = 32)
    private String deliveryChannel;

    @Column(name = "delivery_status_detail", length = 255)
    private String deliveryStatusDetail;

    @Column(name = "source_action_code", length = 32)
    private String sourceActionCode;

    @Column(name = "sent_by_user_id")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID sentByUserId;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public OperationalRecommendationEntity getRecommendation() { return recommendation; }
    public void setRecommendation(OperationalRecommendationEntity recommendation) { this.recommendation = recommendation; }
    public HospitalEntity getHospital() { return hospital; }
    public void setHospital(HospitalEntity hospital) { this.hospital = hospital; }
    public HospitalOperationalGroupEntity getAudienceGroup() { return audienceGroup; }
    public void setAudienceGroup(HospitalOperationalGroupEntity audienceGroup) { this.audienceGroup = audienceGroup; }
    public HospitalOperationalContactEntity getAudienceContact() { return audienceContact; }
    public void setAudienceContact(HospitalOperationalContactEntity audienceContact) { this.audienceContact = audienceContact; }
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
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
