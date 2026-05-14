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
@Table(name = "hospital_inventory_items")
public class HospitalInventoryItemEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private HospitalEntity hospital;

    @Column(name = "item_code", nullable = false, length = 32)
    private String itemCode;

    @Column(name = "item_name", nullable = false, length = 128)
    private String itemName;

    @Column(length = 64)
    private String category;

    @Column(length = 128)
    private String location;

    @Column(name = "current_quantity", nullable = false)
    private int currentQuantity;

    @Column(name = "capacity_quantity", nullable = false)
    private int capacityQuantity;

    @Column(length = 32)
    private String unit;

    @Column(name = "critical_threshold", nullable = false)
    private int criticalThreshold;

    @Column(name = "target_quantity", nullable = false)
    private int targetQuantity;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public HospitalEntity getHospital() { return hospital; }
    public void setHospital(HospitalEntity hospital) { this.hospital = hospital; }
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
