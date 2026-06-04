package com.itesm.application.dto;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

public class HospitalResourcesDto {
    private String hospitalId;
    private LocalDateTime generatedAt;
    private ResourceSummaryDto summary;
    private List<DepartmentDto> departments;
    private List<StaffingProfileDto> staffing;
    private List<InventoryItemDto> inventory;

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public ResourceSummaryDto getSummary() { return summary; }
    public void setSummary(ResourceSummaryDto summary) { this.summary = summary; }
    public List<DepartmentDto> getDepartments() { return departments; }
    public void setDepartments(List<DepartmentDto> departments) { this.departments = departments; }
    public List<StaffingProfileDto> getStaffing() { return staffing; }
    public void setStaffing(List<StaffingProfileDto> staffing) { this.staffing = staffing; }
    public List<InventoryItemDto> getInventory() { return inventory; }
    public void setInventory(List<InventoryItemDto> inventory) { this.inventory = inventory; }

    public static class ResourceSummaryDto {
        private int totalBeds;
        private int availableBeds;
        private int icuTotalBeds;
        private int icuAvailableBeds;
        private int isolationRoomsTotal;
        private int isolationRoomsAvailable;
        private int oxygenCapacityUnits;
        private int oxygenAvailableUnits;
        private int doctorsOnShift;
        private int nursesOnShift;
        private int specialistsOnShift;
        private BigDecimal bedOccupancyPct;
        private BigDecimal icuOccupancyPct;
        private String source;
        private LocalDateTime capturedAt;

        public int getTotalBeds() { return totalBeds; }
        public void setTotalBeds(int totalBeds) { this.totalBeds = totalBeds; }
        public int getAvailableBeds() { return availableBeds; }
        public void setAvailableBeds(int availableBeds) { this.availableBeds = availableBeds; }
        public int getIcuTotalBeds() { return icuTotalBeds; }
        public void setIcuTotalBeds(int icuTotalBeds) { this.icuTotalBeds = icuTotalBeds; }
        public int getIcuAvailableBeds() { return icuAvailableBeds; }
        public void setIcuAvailableBeds(int icuAvailableBeds) { this.icuAvailableBeds = icuAvailableBeds; }
        public int getIsolationRoomsTotal() { return isolationRoomsTotal; }
        public void setIsolationRoomsTotal(int isolationRoomsTotal) { this.isolationRoomsTotal = isolationRoomsTotal; }
        public int getIsolationRoomsAvailable() { return isolationRoomsAvailable; }
        public void setIsolationRoomsAvailable(int isolationRoomsAvailable) { this.isolationRoomsAvailable = isolationRoomsAvailable; }
        public int getOxygenCapacityUnits() { return oxygenCapacityUnits; }
        public void setOxygenCapacityUnits(int oxygenCapacityUnits) { this.oxygenCapacityUnits = oxygenCapacityUnits; }
        public int getOxygenAvailableUnits() { return oxygenAvailableUnits; }
        public void setOxygenAvailableUnits(int oxygenAvailableUnits) { this.oxygenAvailableUnits = oxygenAvailableUnits; }
        public int getDoctorsOnShift() { return doctorsOnShift; }
        public void setDoctorsOnShift(int doctorsOnShift) { this.doctorsOnShift = doctorsOnShift; }
        public int getNursesOnShift() { return nursesOnShift; }
        public void setNursesOnShift(int nursesOnShift) { this.nursesOnShift = nursesOnShift; }
        public int getSpecialistsOnShift() { return specialistsOnShift; }
        public void setSpecialistsOnShift(int specialistsOnShift) { this.specialistsOnShift = specialistsOnShift; }
        public BigDecimal getBedOccupancyPct() { return bedOccupancyPct; }
        public void setBedOccupancyPct(BigDecimal bedOccupancyPct) { this.bedOccupancyPct = bedOccupancyPct; }
        public BigDecimal getIcuOccupancyPct() { return icuOccupancyPct; }
        public void setIcuOccupancyPct(BigDecimal icuOccupancyPct) { this.icuOccupancyPct = icuOccupancyPct; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public LocalDateTime getCapturedAt() { return capturedAt; }
        public void setCapturedAt(LocalDateTime capturedAt) { this.capturedAt = capturedAt; }
    }

    public static class DepartmentDto {
        private String id;
        private String departmentCode;
        private String departmentName;
        private String levelLabel;
        private int totalBeds;
        private int occupiedBeds;
        private int availableBeds;
        private String status;
        private String notes;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getDepartmentCode() { return departmentCode; }
        public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
        public String getLevelLabel() { return levelLabel; }
        public void setLevelLabel(String levelLabel) { this.levelLabel = levelLabel; }
        public int getTotalBeds() { return totalBeds; }
        public void setTotalBeds(int totalBeds) { this.totalBeds = totalBeds; }
        public int getOccupiedBeds() { return occupiedBeds; }
        public void setOccupiedBeds(int occupiedBeds) { this.occupiedBeds = occupiedBeds; }
        public int getAvailableBeds() { return availableBeds; }
        public void setAvailableBeds(int availableBeds) { this.availableBeds = availableBeds; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    public static class StaffingProfileDto {
        private String id;
        private String roleCode;
        private String roleName;
        private int headcount;
        private int onShiftCount;
        private int onCallCount;
        private int standbyCount;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getRoleCode() { return roleCode; }
        public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
        public String getRoleName() { return roleName; }
        public void setRoleName(String roleName) { this.roleName = roleName; }
        public int getHeadcount() { return headcount; }
        public void setHeadcount(int headcount) { this.headcount = headcount; }
        public int getOnShiftCount() { return onShiftCount; }
        public void setOnShiftCount(int onShiftCount) { this.onShiftCount = onShiftCount; }
        public int getOnCallCount() { return onCallCount; }
        public void setOnCallCount(int onCallCount) { this.onCallCount = onCallCount; }
        public int getStandbyCount() { return standbyCount; }
        public void setStandbyCount(int standbyCount) { this.standbyCount = standbyCount; }
    }

    public static class InventoryItemDto {
        private String id;
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

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
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
    }
}
