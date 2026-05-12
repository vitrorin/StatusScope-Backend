package com.itesm.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class HospitalInventoryMovement {
    private UUID id;
    private UUID hospitalId;
    private UUID inventoryItemId;
    private String movementType;
    private int quantityDelta;
    private String unit;
    private String notes;
    private UUID relatedSupplyRequestId;
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }
    public UUID getInventoryItemId() { return inventoryItemId; }
    public void setInventoryItemId(UUID inventoryItemId) { this.inventoryItemId = inventoryItemId; }
    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }
    public int getQuantityDelta() { return quantityDelta; }
    public void setQuantityDelta(int quantityDelta) { this.quantityDelta = quantityDelta; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public UUID getRelatedSupplyRequestId() { return relatedSupplyRequestId; }
    public void setRelatedSupplyRequestId(UUID relatedSupplyRequestId) { this.relatedSupplyRequestId = relatedSupplyRequestId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
