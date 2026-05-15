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
@Table(name = "hospital_resource_snapshots")
public class HospitalResourceSnapshotEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private HospitalEntity hospital;

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;

    @Column(name = "total_beds", nullable = false)
    private int totalBeds;

    @Column(name = "available_beds", nullable = false)
    private int availableBeds;

    @Column(name = "icu_total_beds", nullable = false)
    private int icuTotalBeds;

    @Column(name = "icu_available_beds", nullable = false)
    private int icuAvailableBeds;

    @Column(name = "isolation_rooms_total", nullable = false)
    private int isolationRoomsTotal;

    @Column(name = "isolation_rooms_available", nullable = false)
    private int isolationRoomsAvailable;

    @Column(name = "oxygen_capacity_units", nullable = false)
    private int oxygenCapacityUnits;

    @Column(name = "oxygen_available_units", nullable = false)
    private int oxygenAvailableUnits;

    @Column(name = "doctors_on_shift", nullable = false)
    private int doctorsOnShift;

    @Column(name = "nurses_on_shift", nullable = false)
    private int nursesOnShift;

    @Column(name = "specialists_on_shift", nullable = false)
    private int specialistsOnShift;

    @Column(nullable = false, length = 16)
    private String source;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public HospitalEntity getHospital() { return hospital; }
    public void setHospital(HospitalEntity hospital) { this.hospital = hospital; }
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
