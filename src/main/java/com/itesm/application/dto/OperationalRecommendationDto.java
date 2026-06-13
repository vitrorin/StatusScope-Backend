package com.itesm.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class OperationalRecommendationDto {
    private String id;
    private String hospitalId;
    private String sourceAlertId;
    private String sourceOutbreakId;
    private String type;
    private String severity;
    private String status;
    private String category;
    private String title;
    private String description;
    private String expectedImpact;
    private String urgencyWindow;
    private BigDecimal confidenceScore;
    private Map<String, LocalizedContentDto> translations;
    private String imageMode;
    private List<String> rationale;
    private List<String> recommendedActions;
    private List<String> affectedDepartments;
    private List<String> affectedResources;
    private String displayCategoryLabel;
    private String displaySeverityLabel;
    private String displayStatusLabel;
    private RecommendationTargetDto primaryDepartment;
    private RecommendationTargetDto primaryStaffingProfile;
    private RecommendationTargetDto primaryInventoryItem;
    private List<RecommendationActionDto> availableActions;
    private List<String> allowedStatusTransitions;
    private String primaryActionCode;
    private LocalDateTime expiresAt;
    private RecommendationTargetDto assignedOwner;
    private String createdByMode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private List<AuditEntryDto> auditTrail;
    private List<TaskDto> tasks;
    private List<NotificationDto> notifications;
    private List<SupplyRequestItemDto> supplyRequests;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }
    public String getSourceAlertId() { return sourceAlertId; }
    public void setSourceAlertId(String sourceAlertId) { this.sourceAlertId = sourceAlertId; }
    public String getSourceOutbreakId() { return sourceOutbreakId; }
    public void setSourceOutbreakId(String sourceOutbreakId) { this.sourceOutbreakId = sourceOutbreakId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getExpectedImpact() { return expectedImpact; }
    public void setExpectedImpact(String expectedImpact) { this.expectedImpact = expectedImpact; }
    public String getUrgencyWindow() { return urgencyWindow; }
    public void setUrgencyWindow(String urgencyWindow) { this.urgencyWindow = urgencyWindow; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    public Map<String, LocalizedContentDto> getTranslations() { return translations; }
    public void setTranslations(Map<String, LocalizedContentDto> translations) { this.translations = translations; }
    public String getImageMode() { return imageMode; }
    public void setImageMode(String imageMode) { this.imageMode = imageMode; }
    public List<String> getRationale() { return rationale; }
    public void setRationale(List<String> rationale) { this.rationale = rationale; }
    public List<String> getRecommendedActions() { return recommendedActions; }
    public void setRecommendedActions(List<String> recommendedActions) { this.recommendedActions = recommendedActions; }
    public List<String> getAffectedDepartments() { return affectedDepartments; }
    public void setAffectedDepartments(List<String> affectedDepartments) { this.affectedDepartments = affectedDepartments; }
    public List<String> getAffectedResources() { return affectedResources; }
    public void setAffectedResources(List<String> affectedResources) { this.affectedResources = affectedResources; }
    public String getDisplayCategoryLabel() { return displayCategoryLabel; }
    public void setDisplayCategoryLabel(String displayCategoryLabel) { this.displayCategoryLabel = displayCategoryLabel; }
    public String getDisplaySeverityLabel() { return displaySeverityLabel; }
    public void setDisplaySeverityLabel(String displaySeverityLabel) { this.displaySeverityLabel = displaySeverityLabel; }
    public String getDisplayStatusLabel() { return displayStatusLabel; }
    public void setDisplayStatusLabel(String displayStatusLabel) { this.displayStatusLabel = displayStatusLabel; }
    public RecommendationTargetDto getPrimaryDepartment() { return primaryDepartment; }
    public void setPrimaryDepartment(RecommendationTargetDto primaryDepartment) { this.primaryDepartment = primaryDepartment; }
    public RecommendationTargetDto getPrimaryStaffingProfile() { return primaryStaffingProfile; }
    public void setPrimaryStaffingProfile(RecommendationTargetDto primaryStaffingProfile) { this.primaryStaffingProfile = primaryStaffingProfile; }
    public RecommendationTargetDto getPrimaryInventoryItem() { return primaryInventoryItem; }
    public void setPrimaryInventoryItem(RecommendationTargetDto primaryInventoryItem) { this.primaryInventoryItem = primaryInventoryItem; }
    public List<RecommendationActionDto> getAvailableActions() { return availableActions; }
    public void setAvailableActions(List<RecommendationActionDto> availableActions) { this.availableActions = availableActions; }
    public List<String> getAllowedStatusTransitions() { return allowedStatusTransitions; }
    public void setAllowedStatusTransitions(List<String> allowedStatusTransitions) { this.allowedStatusTransitions = allowedStatusTransitions; }
    public String getPrimaryActionCode() { return primaryActionCode; }
    public void setPrimaryActionCode(String primaryActionCode) { this.primaryActionCode = primaryActionCode; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public RecommendationTargetDto getAssignedOwner() { return assignedOwner; }
    public void setAssignedOwner(RecommendationTargetDto assignedOwner) { this.assignedOwner = assignedOwner; }
    public String getCreatedByMode() { return createdByMode; }
    public void setCreatedByMode(String createdByMode) { this.createdByMode = createdByMode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public List<AuditEntryDto> getAuditTrail() { return auditTrail; }
    public void setAuditTrail(List<AuditEntryDto> auditTrail) { this.auditTrail = auditTrail; }
    public List<TaskDto> getTasks() { return tasks; }
    public void setTasks(List<TaskDto> tasks) { this.tasks = tasks; }
    public List<NotificationDto> getNotifications() { return notifications; }
    public void setNotifications(List<NotificationDto> notifications) { this.notifications = notifications; }
    public List<SupplyRequestItemDto> getSupplyRequests() { return supplyRequests; }
    public void setSupplyRequests(List<SupplyRequestItemDto> supplyRequests) { this.supplyRequests = supplyRequests; }

    public static class AuditEntryDto {
        private String id;
        private String eventType;
        private String eventLabel;
        private LocalDateTime createdAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public String getEventLabel() { return eventLabel; }
        public void setEventLabel(String eventLabel) { this.eventLabel = eventLabel; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class RecommendationActionDto {
        private String code;
        private String label;
        private String style;
        private boolean enabled;
        private String disabledReason;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getStyle() { return style; }
        public void setStyle(String style) { this.style = style; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getDisabledReason() { return disabledReason; }
        public void setDisabledReason(String disabledReason) { this.disabledReason = disabledReason; }
    }

    public static class LocalizedContentDto {
        private String title;
        private String description;
        private String expectedImpact;
        private String urgencyWindow;
        private List<String> rationale;
        private List<String> recommendedActions;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getExpectedImpact() { return expectedImpact; }
        public void setExpectedImpact(String expectedImpact) { this.expectedImpact = expectedImpact; }
        public String getUrgencyWindow() { return urgencyWindow; }
        public void setUrgencyWindow(String urgencyWindow) { this.urgencyWindow = urgencyWindow; }
        public List<String> getRationale() { return rationale; }
        public void setRationale(List<String> rationale) { this.rationale = rationale; }
        public List<String> getRecommendedActions() { return recommendedActions; }
        public void setRecommendedActions(List<String> recommendedActions) { this.recommendedActions = recommendedActions; }
    }

    public static class RecommendationTargetDto {
        private String id;
        private String label;
        private String type;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    public static class TaskDto {
        private String id;
        private String ownerContactId;
        private String ownerGroupId;
        private String ownerLabel;
        private String departmentLabel;
        private String priority;
        private String status;
        private String sourceActionCode;
        private LocalDateTime deadlineAt;
        private String notes;
        private LocalDateTime createdAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getOwnerContactId() { return ownerContactId; }
        public void setOwnerContactId(String ownerContactId) { this.ownerContactId = ownerContactId; }
        public String getOwnerGroupId() { return ownerGroupId; }
        public void setOwnerGroupId(String ownerGroupId) { this.ownerGroupId = ownerGroupId; }
        public String getOwnerLabel() { return ownerLabel; }
        public void setOwnerLabel(String ownerLabel) { this.ownerLabel = ownerLabel; }
        public String getDepartmentLabel() { return departmentLabel; }
        public void setDepartmentLabel(String departmentLabel) { this.departmentLabel = departmentLabel; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getSourceActionCode() { return sourceActionCode; }
        public void setSourceActionCode(String sourceActionCode) { this.sourceActionCode = sourceActionCode; }
        public LocalDateTime getDeadlineAt() { return deadlineAt; }
        public void setDeadlineAt(LocalDateTime deadlineAt) { this.deadlineAt = deadlineAt; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class NotificationDto {
        private String id;
        private String audienceGroupId;
        private String audienceContactId;
        private String audienceType;
        private String audienceDepartmentCode;
        private String audienceLabel;
        private String message;
        private String status;
        private String deliveryChannel;
        private String deliveryStatusDetail;
        private String sourceActionCode;
        private RecipientSummaryDto recipientSummary;
        private List<NotificationRecipientDto> recipients;
        private LocalDateTime sentAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getAudienceGroupId() { return audienceGroupId; }
        public void setAudienceGroupId(String audienceGroupId) { this.audienceGroupId = audienceGroupId; }
        public String getAudienceContactId() { return audienceContactId; }
        public void setAudienceContactId(String audienceContactId) { this.audienceContactId = audienceContactId; }
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
        public RecipientSummaryDto getRecipientSummary() { return recipientSummary; }
        public void setRecipientSummary(RecipientSummaryDto recipientSummary) { this.recipientSummary = recipientSummary; }
        public List<NotificationRecipientDto> getRecipients() { return recipients; }
        public void setRecipients(List<NotificationRecipientDto> recipients) { this.recipients = recipients; }
        public LocalDateTime getSentAt() { return sentAt; }
        public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    }

    public static class RecipientSummaryDto {
        private int total;
        private int sent;
        private int failed;

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getSent() { return sent; }
        public void setSent(int sent) { this.sent = sent; }
        public int getFailed() { return failed; }
        public void setFailed(int failed) { this.failed = failed; }
    }

    public static class NotificationRecipientDto {
        private String id;
        private String contactId;
        private String recipientName;
        private String recipientEmail;
        private String status;
        private String deliveryStatusDetail;
        private LocalDateTime deliveredAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getContactId() { return contactId; }
        public void setContactId(String contactId) { this.contactId = contactId; }
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

    public static class SupplyRequestItemDto {
        private String id;
        private String inventoryItemId;
        private String supplyTypeLabel;
        private int quantity;
        private String unit;
        private String destination;
        private String suggestedSupplier;
        private String status;
        private String sourceActionCode;
        private String priority;
        private LocalDateTime requestedNeededBy;
        private LocalDateTime createdAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getInventoryItemId() { return inventoryItemId; }
        public void setInventoryItemId(String inventoryItemId) { this.inventoryItemId = inventoryItemId; }
        public String getSupplyTypeLabel() { return supplyTypeLabel; }
        public void setSupplyTypeLabel(String supplyTypeLabel) { this.supplyTypeLabel = supplyTypeLabel; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
        public String getSuggestedSupplier() { return suggestedSupplier; }
        public void setSuggestedSupplier(String suggestedSupplier) { this.suggestedSupplier = suggestedSupplier; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getSourceActionCode() { return sourceActionCode; }
        public void setSourceActionCode(String sourceActionCode) { this.sourceActionCode = sourceActionCode; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public LocalDateTime getRequestedNeededBy() { return requestedNeededBy; }
        public void setRequestedNeededBy(LocalDateTime requestedNeededBy) { this.requestedNeededBy = requestedNeededBy; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}
