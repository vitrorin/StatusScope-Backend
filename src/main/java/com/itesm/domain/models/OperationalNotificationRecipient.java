package com.itesm.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class OperationalNotificationRecipient {
    private UUID id;
    private UUID notificationId;
    private UUID contactId;
    private String recipientName;
    private String recipientEmail;
    private String status;
    private String deliveryStatusDetail;
    private LocalDateTime deliveredAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getNotificationId() { return notificationId; }
    public void setNotificationId(UUID notificationId) { this.notificationId = notificationId; }
    public UUID getContactId() { return contactId; }
    public void setContactId(UUID contactId) { this.contactId = contactId; }
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
