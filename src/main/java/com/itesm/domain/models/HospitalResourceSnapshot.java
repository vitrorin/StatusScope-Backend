package com.itesm.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class HospitalResourceSnapshot {
    private UUID id;
    private UUID hospitalId;
    private LocalDateTime capturedAt;
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
    private String source;
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }
    public LocalDateTime getCapturedAt() { return capturedAt; }
    public void setCapturedAt(LocalDateTime capturedAt) { this.capturedAt = capturedAt; }
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
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
