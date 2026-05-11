package com.itesm.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    private String imageMode;
    private List<String> rationale;
    private List<String> recommendedActions;
    private List<String> affectedDepartments;
    private List<String> affectedResources;
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

    public static class TaskDto {
        private String id;
        private String ownerLabel;
        private String departmentLabel;
        private String priority;
        private String status;
        private LocalDateTime deadlineAt;
        private String notes;
        private LocalDateTime createdAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getOwnerLabel() { return ownerLabel; }
        public void setOwnerLabel(String ownerLabel) { this.ownerLabel = ownerLabel; }
        public String getDepartmentLabel() { return departmentLabel; }
        public void setDepartmentLabel(String departmentLabel) { this.departmentLabel = departmentLabel; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getDeadlineAt() { return deadlineAt; }
        public void setDeadlineAt(LocalDateTime deadlineAt) { this.deadlineAt = deadlineAt; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class NotificationDto {
        private String id;
        private String audienceLabel;
        private String message;
        private String status;
        private LocalDateTime sentAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getAudienceLabel() { return audienceLabel; }
        public void setAudienceLabel(String audienceLabel) { this.audienceLabel = audienceLabel; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getSentAt() { return sentAt; }
        public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    }

    public static class SupplyRequestItemDto {
        private String id;
        private String supplyTypeLabel;
        private int quantity;
        private String unit;
        private String destination;
        private String suggestedSupplier;
        private String status;
        private LocalDateTime createdAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
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
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}
