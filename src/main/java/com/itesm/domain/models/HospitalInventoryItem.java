package com.itesm.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class HospitalInventoryItem {
    private UUID id;
    private UUID hospitalId;
    private String itemCode;
    private String itemName;
    private String category;
    private String location;
    private int currentQuantity;
    private int capacityQuantity;
    private String unit;
    private int criticalThreshold;
    private int targetQuantity;
    private String status;
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public int getCurrentQuantity() { return currentQuantity; }
    public void setCurrentQuantity(int currentQuantity) { this.currentQuantity = currentQuantity; }
    public int getCapacityQuantity() { return capacityQuantity; }
    public void setCapacityQuantity(int capacityQuantity) { this.capacityQuantity = capacityQuantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public int getCriticalThreshold() { return criticalThreshold; }
    public void setCriticalThreshold(int criticalThreshold) { this.criticalThreshold = criticalThreshold; }
    public int getTargetQuantity() { return targetQuantity; }
    public void setTargetQuantity(int targetQuantity) { this.targetQuantity = targetQuantity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
