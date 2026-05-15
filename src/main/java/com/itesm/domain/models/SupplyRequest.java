package com.itesm.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class SupplyRequest {
    private UUID id;
    private UUID recommendationId;
    private UUID hospitalId;
    private UUID inventoryItemId;
    private String supplyTypeLabel;
    private int quantity;
    private String unit;
    private String destination;
    private String suggestedSupplier;
    private String status;
    private String sourceActionCode;
    private String priority;
    private LocalDateTime requestedNeededBy;
    private UUID linkedRecommendationInventoryItemId;
    private UUID requestedByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getRecommendationId() { return recommendationId; }
    public void setRecommendationId(UUID recommendationId) { this.recommendationId = recommendationId; }
    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }
    public UUID getInventoryItemId() { return inventoryItemId; }
    public void setInventoryItemId(UUID inventoryItemId) { this.inventoryItemId = inventoryItemId; }
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
    public UUID getLinkedRecommendationInventoryItemId() { return linkedRecommendationInventoryItemId; }
    public void setLinkedRecommendationInventoryItemId(UUID linkedRecommendationInventoryItemId) { this.linkedRecommendationInventoryItemId = linkedRecommendationInventoryItemId; }
    public UUID getRequestedByUserId() { return requestedByUserId; }
    public void setRequestedByUserId(UUID requestedByUserId) { this.requestedByUserId = requestedByUserId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
