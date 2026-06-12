package com.itesm.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SystemDashboardSummaryDto {
    private LocalDateTime generatedAt;
    private List<SystemMetricDto> metrics = new ArrayList<>();
    private List<SystemActivityPointDto> userActivity = new ArrayList<>();
    private List<SystemRegionalDistributionDto> regionalDistribution = new ArrayList<>();
    private List<SystemEventDto> recentEvents = new ArrayList<>();
    private List<SystemHospitalOutbreakDto> hospitalOutbreaks = new ArrayList<>();
    private List<SystemHospitalUserMetricDto> hospitalUserMetrics = new ArrayList<>();

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public List<SystemMetricDto> getMetrics() { return metrics; }
    public void setMetrics(List<SystemMetricDto> metrics) { this.metrics = metrics; }

    public List<SystemActivityPointDto> getUserActivity() { return userActivity; }
    public void setUserActivity(List<SystemActivityPointDto> userActivity) { this.userActivity = userActivity; }

    public List<SystemRegionalDistributionDto> getRegionalDistribution() { return regionalDistribution; }
    public void setRegionalDistribution(List<SystemRegionalDistributionDto> regionalDistribution) { this.regionalDistribution = regionalDistribution; }

    public List<SystemEventDto> getRecentEvents() { return recentEvents; }
    public void setRecentEvents(List<SystemEventDto> recentEvents) { this.recentEvents = recentEvents; }

    public List<SystemHospitalOutbreakDto> getHospitalOutbreaks() { return hospitalOutbreaks; }
    public void setHospitalOutbreaks(List<SystemHospitalOutbreakDto> hospitalOutbreaks) { this.hospitalOutbreaks = hospitalOutbreaks; }

    public List<SystemHospitalUserMetricDto> getHospitalUserMetrics() { return hospitalUserMetrics; }
    public void setHospitalUserMetrics(List<SystemHospitalUserMetricDto> hospitalUserMetrics) { this.hospitalUserMetrics = hospitalUserMetrics; }

    public static class SystemMetricDto {
        private String id;
        private String title;
        private String value;
        private String detail;
        private String status;
        private String iconKey;

        public SystemMetricDto() {}

        public SystemMetricDto(String id, String title, String value, String detail, String status, String iconKey) {
            this.id = id;
            this.title = title;
            this.value = value;
            this.detail = detail;
            this.status = status;
            this.iconKey = iconKey;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getDetail() { return detail; }
        public void setDetail(String detail) { this.detail = detail; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getIconKey() { return iconKey; }
        public void setIconKey(String iconKey) { this.iconKey = iconKey; }
    }

    public static class SystemActivityPointDto {
        private String label;
        private LocalDate date;
        private int value;
        private int adminValue;
        private int doctorValue;

        public SystemActivityPointDto() {}

        public SystemActivityPointDto(String label, int value) {
            this.label = label;
            this.value = value;
        }

        public SystemActivityPointDto(String label, LocalDate date, int value, int adminValue, int doctorValue) {
            this.label = label;
            this.date = date;
            this.value = value;
            this.adminValue = adminValue;
            this.doctorValue = doctorValue;
        }

        public SystemActivityPointDto(String label, int value, int adminValue, int doctorValue) {
            this.label = label;
            this.value = value;
            this.adminValue = adminValue;
            this.doctorValue = doctorValue;
        }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
        public int getAdminValue() { return adminValue; }
        public void setAdminValue(int adminValue) { this.adminValue = adminValue; }
        public int getDoctorValue() { return doctorValue; }
        public void setDoctorValue(int doctorValue) { this.doctorValue = doctorValue; }
    }

    public static class SystemRegionalDistributionDto {
        private String label;
        private int value;
        private int percent;

        public SystemRegionalDistributionDto() {}

        public SystemRegionalDistributionDto(String label, int value, int percent) {
            this.label = label;
            this.value = value;
            this.percent = percent;
        }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
        public int getPercent() { return percent; }
        public void setPercent(int percent) { this.percent = percent; }
    }

    public static class SystemEventDto {
        private String id;
        private String title;
        private String detail;
        private String type;
        private LocalDateTime occurredAt;

        public SystemEventDto() {}

        public SystemEventDto(String id, String title, String detail, String type, LocalDateTime occurredAt) {
            this.id = id;
            this.title = title;
            this.detail = detail;
            this.type = type;
            this.occurredAt = occurredAt;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDetail() { return detail; }
        public void setDetail(String detail) { this.detail = detail; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public LocalDateTime getOccurredAt() { return occurredAt; }
        public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    }

    public static class SystemHospitalOutbreakDto {
        private String id;
        private String code;
        private String name;
        private String municipalityName;
        private String stateName;
        private boolean active;
        private Double latitude;
        private Double longitude;
        private int nearbyActiveOutbreakCount;
        private double radiusKm;
        private List<SystemNearbyOutbreakDto> nearbyOutbreaks = new ArrayList<>();

        public SystemHospitalOutbreakDto() {}

        public SystemHospitalOutbreakDto(
                String id,
                String code,
                String name,
                String municipalityName,
                String stateName,
                boolean active,
                Double latitude,
                Double longitude,
                int nearbyActiveOutbreakCount,
                double radiusKm,
                List<SystemNearbyOutbreakDto> nearbyOutbreaks) {
            this.id = id;
            this.code = code;
            this.name = name;
            this.municipalityName = municipalityName;
            this.stateName = stateName;
            this.active = active;
            this.latitude = latitude;
            this.longitude = longitude;
            this.nearbyActiveOutbreakCount = nearbyActiveOutbreakCount;
            this.radiusKm = radiusKm;
            this.nearbyOutbreaks = nearbyOutbreaks;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getMunicipalityName() { return municipalityName; }
        public void setMunicipalityName(String municipalityName) { this.municipalityName = municipalityName; }
        public String getStateName() { return stateName; }
        public void setStateName(String stateName) { this.stateName = stateName; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
        public int getNearbyActiveOutbreakCount() { return nearbyActiveOutbreakCount; }
        public void setNearbyActiveOutbreakCount(int nearbyActiveOutbreakCount) { this.nearbyActiveOutbreakCount = nearbyActiveOutbreakCount; }
        public double getRadiusKm() { return radiusKm; }
        public void setRadiusKm(double radiusKm) { this.radiusKm = radiusKm; }
        public List<SystemNearbyOutbreakDto> getNearbyOutbreaks() { return nearbyOutbreaks; }
        public void setNearbyOutbreaks(List<SystemNearbyOutbreakDto> nearbyOutbreaks) { this.nearbyOutbreaks = nearbyOutbreaks; }
    }

    public static class SystemNearbyOutbreakDto {
        private String id;
        private String diseaseName;
        private String municipalityName;
        private String stateName;
        private int caseCount;
        private String confirmationStatus;
        private String severity;
        private double distanceKm;
        private LocalDateTime startedAt;

        public SystemNearbyOutbreakDto() {}

        public SystemNearbyOutbreakDto(
                String id,
                String diseaseName,
                String municipalityName,
                String stateName,
                int caseCount,
                String confirmationStatus,
                String severity,
                double distanceKm,
                LocalDateTime startedAt) {
            this.id = id;
            this.diseaseName = diseaseName;
            this.municipalityName = municipalityName;
            this.stateName = stateName;
            this.caseCount = caseCount;
            this.confirmationStatus = confirmationStatus;
            this.severity = severity;
            this.distanceKm = distanceKm;
            this.startedAt = startedAt;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getDiseaseName() { return diseaseName; }
        public void setDiseaseName(String diseaseName) { this.diseaseName = diseaseName; }
        public String getMunicipalityName() { return municipalityName; }
        public void setMunicipalityName(String municipalityName) { this.municipalityName = municipalityName; }
        public String getStateName() { return stateName; }
        public void setStateName(String stateName) { this.stateName = stateName; }
        public int getCaseCount() { return caseCount; }
        public void setCaseCount(int caseCount) { this.caseCount = caseCount; }
        public String getConfirmationStatus() { return confirmationStatus; }
        public void setConfirmationStatus(String confirmationStatus) { this.confirmationStatus = confirmationStatus; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public double getDistanceKm() { return distanceKm; }
        public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
        public LocalDateTime getStartedAt() { return startedAt; }
        public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    }

    public static class SystemHospitalUserMetricDto {
        private String hospitalId;
        private String hospitalName;
        private String municipalityName;
        private String stateName;
        private int totalUsers;
        private int activeUsers;
        private int adminUsers;
        private int doctorUsers;
        private int inactiveUsers;

        public SystemHospitalUserMetricDto() {}

        public SystemHospitalUserMetricDto(
                String hospitalId,
                String hospitalName,
                String municipalityName,
                String stateName,
                int totalUsers,
                int activeUsers,
                int adminUsers,
                int doctorUsers,
                int inactiveUsers) {
            this.hospitalId = hospitalId;
            this.hospitalName = hospitalName;
            this.municipalityName = municipalityName;
            this.stateName = stateName;
            this.totalUsers = totalUsers;
            this.activeUsers = activeUsers;
            this.adminUsers = adminUsers;
            this.doctorUsers = doctorUsers;
            this.inactiveUsers = inactiveUsers;
        }

        public String getHospitalId() { return hospitalId; }
        public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }
        public String getHospitalName() { return hospitalName; }
        public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }
        public String getMunicipalityName() { return municipalityName; }
        public void setMunicipalityName(String municipalityName) { this.municipalityName = municipalityName; }
        public String getStateName() { return stateName; }
        public void setStateName(String stateName) { this.stateName = stateName; }
        public int getTotalUsers() { return totalUsers; }
        public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }
        public int getActiveUsers() { return activeUsers; }
        public void setActiveUsers(int activeUsers) { this.activeUsers = activeUsers; }
        public int getAdminUsers() { return adminUsers; }
        public void setAdminUsers(int adminUsers) { this.adminUsers = adminUsers; }
        public int getDoctorUsers() { return doctorUsers; }
        public void setDoctorUsers(int doctorUsers) { this.doctorUsers = doctorUsers; }
        public int getInactiveUsers() { return inactiveUsers; }
        public void setInactiveUsers(int inactiveUsers) { this.inactiveUsers = inactiveUsers; }
    }
}
