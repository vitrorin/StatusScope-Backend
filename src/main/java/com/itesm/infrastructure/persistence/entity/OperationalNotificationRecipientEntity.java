package com.itesm.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "operational_notification_recipients")
public class OperationalNotificationRecipientEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private OperationalNotificationEntity notification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private HospitalOperationalContactEntity contact;

    @Column(name = "recipient_name", length = 255)
    private String recipientName;

    @Column(name = "recipient_email", length = 255)
    private String recipientEmail;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "delivery_status_detail", length = 255)
    private String deliveryStatusDetail;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public OperationalNotificationEntity getNotification() { return notification; }
    public void setNotification(OperationalNotificationEntity notification) { this.notification = notification; }
    public HospitalOperationalContactEntity getContact() { return contact; }
    public void setContact(HospitalOperationalContactEntity contact) { this.contact = contact; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDeliveryStatusDetail() { return deliveryStatusDetail; }
    public void setDeliveryStatusDetail(String deliveryStatusDetail) { this.deliveryStatusDetail = deliveryStatusDetail; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
}
