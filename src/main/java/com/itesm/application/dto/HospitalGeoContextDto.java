package com.itesm.application.dto;

import com.itesm.domain.models.State;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class HospitalGeoContextDto {
    private UUID hospitalId;
    private UUID municipalityId;
    private String municipalityName;
    private UUID stateId;
    private String stateName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private double radiusKm;
    private List<UUID> includedMunicipalityIds;
    private List<State> nearbyStates;

    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }

    public UUID getMunicipalityId() { return municipalityId; }
    public void setMunicipalityId(UUID municipalityId) { this.municipalityId = municipalityId; }

    public String getMunicipalityName() { return municipalityName; }
    public void setMunicipalityName(String municipalityName) { this.municipalityName = municipalityName; }

    public UUID getStateId() { return stateId; }
    public void setStateId(UUID stateId) { this.stateId = stateId; }

    public String getStateName() { return stateName; }
    public void setStateName(String stateName) { this.stateName = stateName; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public double getRadiusKm() { return radiusKm; }
    public void setRadiusKm(double radiusKm) { this.radiusKm = radiusKm; }

    public List<UUID> getIncludedMunicipalityIds() { return includedMunicipalityIds; }
    public void setIncludedMunicipalityIds(List<UUID> includedMunicipalityIds) { this.includedMunicipalityIds = includedMunicipalityIds; }

    public List<State> getNearbyStates() { return nearbyStates; }
    public void setNearbyStates(List<State> nearbyStates) { this.nearbyStates = nearbyStates; }
}
