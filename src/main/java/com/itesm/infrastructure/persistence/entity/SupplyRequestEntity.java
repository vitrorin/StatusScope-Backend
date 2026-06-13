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
@Table(name = "supply_requests")
public class SupplyRequestEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id")
    private OperationalRecommendationEntity recommendation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private HospitalEntity hospital;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id")
    private HospitalInventoryItemEntity inventoryItem;

    @Column(name = "supply_type_label", length = 255)
    private String supplyTypeLabel;

    @Column(nullable = false)
    private int quantity;

    @Column(length = 32)
    private String unit;

    @Column(length = 255)
    private String destination;

    @Column(name = "suggested_supplier", length = 255)
    private String suggestedSupplier;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "source_action_code", length = 32)
    private String sourceActionCode;

    @Column(length = 16)
    private String priority;

    @Column(name = "requested_needed_by")
    private LocalDateTime requestedNeededBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_recommendation_inventory_item_id")
    private HospitalInventoryItemEntity linkedRecommendationInventoryItem;

    @Column(name = "requested_by_user_id")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID requestedByUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public OperationalRecommendationEntity getRecommendation() { return recommendation; }
    public void setRecommendation(OperationalRecommendationEntity recommendation) { this.recommendation = recommendation; }
    public HospitalEntity getHospital() { return hospital; }
    public void setHospital(HospitalEntity hospital) { this.hospital = hospital; }
    public HospitalInventoryItemEntity getInventoryItem() { return inventoryItem; }
    public void setInventoryItem(HospitalInventoryItemEntity inventoryItem) { this.inventoryItem = inventoryItem; }
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
    public HospitalInventoryItemEntity getLinkedRecommendationInventoryItem() { return linkedRecommendationInventoryItem; }
    public void setLinkedRecommendationInventoryItem(HospitalInventoryItemEntity linkedRecommendationInventoryItem) { this.linkedRecommendationInventoryItem = linkedRecommendationInventoryItem; }
    public UUID getRequestedByUserId() { return requestedByUserId; }
    public void setRequestedByUserId(UUID requestedByUserId) { this.requestedByUserId = requestedByUserId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
