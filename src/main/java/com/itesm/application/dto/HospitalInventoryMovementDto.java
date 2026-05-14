package com.itesm.application.dto;

import java.time.LocalDateTime;

public class HospitalInventoryMovementDto {
    private String id;
    private String inventoryItemId;
    private String movementType;
    private int quantityDelta;
    private String unit;
    private String notes;
    private String relatedSupplyRequestId;
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getInventoryItemId() { return inventoryItemId; }
    public void setInventoryItemId(String inventoryItemId) { this.inventoryItemId = inventoryItemId; }
    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }
    public int getQuantityDelta() { return quantityDelta; }
    public void setQuantityDelta(int quantityDelta) { this.quantityDelta = quantityDelta; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getRelatedSupplyRequestId() { return relatedSupplyRequestId; }
    public void setRelatedSupplyRequestId(String relatedSupplyRequestId) { this.relatedSupplyRequestId = relatedSupplyRequestId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
