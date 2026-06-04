package com.itesm.application.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AdminDashboardSummaryDto {
    private String hospitalName;
    private String municipalityName;
    private String stateName;
    private LocalDateTime generatedAt;
    private List<AdminMetricCardDto> topCards;
    private List<AdminDashboardAlertDto> alerts;
    private List<AdminMapZoneDto> mapZones;
    private List<AdminRecommendedActionDto> recommendedActions;

    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }
    public String getMunicipalityName() { return municipalityName; }
    public void setMunicipalityName(String municipalityName) { this.municipalityName = municipalityName; }
    public String getStateName() { return stateName; }
    public void setStateName(String stateName) { this.stateName = stateName; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public List<AdminMetricCardDto> getTopCards() { return topCards; }
    public void setTopCards(List<AdminMetricCardDto> topCards) { this.topCards = topCards; }
    public List<AdminDashboardAlertDto> getAlerts() { return alerts; }
    public void setAlerts(List<AdminDashboardAlertDto> alerts) { this.alerts = alerts; }
    public List<AdminMapZoneDto> getMapZones() { return mapZones; }
    public void setMapZones(List<AdminMapZoneDto> mapZones) { this.mapZones = mapZones; }
    public List<AdminRecommendedActionDto> getRecommendedActions() { return recommendedActions; }
    public void setRecommendedActions(List<AdminRecommendedActionDto> recommendedActions) { this.recommendedActions = recommendedActions; }

    public static class AdminMetricCardDto {
        private String id;
        private String title;
        private String value;
        private String subtitle;
        private String status;
        private String badge;
        private String iconKey;
        private String displayVariant;
        private Integer progressPercent;
        private String progressColorToken;
        private String badgeTone;
        private String recommendedActionId;
        private String actionLabel;

        public AdminMetricCardDto() {}
        public AdminMetricCardDto(String id, String title, String value, String subtitle, String status, String badge, String iconKey) {
            this.id = id; this.title = title; this.value = value;
            this.subtitle = subtitle; this.status = status; this.badge = badge; this.iconKey = iconKey;
        }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getSubtitle() { return subtitle; }
        public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getBadge() { return badge; }
        public void setBadge(String badge) { this.badge = badge; }
        public String getIconKey() { return iconKey; }
        public void setIconKey(String iconKey) { this.iconKey = iconKey; }
        public String getDisplayVariant() { return displayVariant; }
        public void setDisplayVariant(String displayVariant) { this.displayVariant = displayVariant; }
        public Integer getProgressPercent() { return progressPercent; }
        public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }
        public String getProgressColorToken() { return progressColorToken; }
        public void setProgressColorToken(String progressColorToken) { this.progressColorToken = progressColorToken; }
        public String getBadgeTone() { return badgeTone; }
        public void setBadgeTone(String badgeTone) { this.badgeTone = badgeTone; }
        public String getRecommendedActionId() { return recommendedActionId; }
        public void setRecommendedActionId(String recommendedActionId) { this.recommendedActionId = recommendedActionId; }
        public String getActionLabel() { return actionLabel; }
        public void setActionLabel(String actionLabel) { this.actionLabel = actionLabel; }
    }

    public static class AdminDashboardAlertDto {
        private String id;
        private String disease;
        private String severity;
        private String location;
        private String message;
        private int caseCount;
        private LocalDateTime createdAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getDisease() { return disease; }
        public void setDisease(String disease) { this.disease = disease; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public int getCaseCount() { return caseCount; }
        public void setCaseCount(int caseCount) { this.caseCount = caseCount; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class AdminMapZoneDto {
        private String municipalityId;
        private String municipalityName;
        private String status;
        private int outbreakCount;
        private double latitude;
        private double longitude;
        private String displayPriorityLabel;
        private String recommendedActionId;
        private String displayColorToken;

        public String getMunicipalityId() { return municipalityId; }
        public void setMunicipalityId(String municipalityId) { this.municipalityId = municipalityId; }
        public String getMunicipalityName() { return municipalityName; }
        public void setMunicipalityName(String municipalityName) { this.municipalityName = municipalityName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getOutbreakCount() { return outbreakCount; }
        public void setOutbreakCount(int outbreakCount) { this.outbreakCount = outbreakCount; }
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        public String getDisplayPriorityLabel() { return displayPriorityLabel; }
        public void setDisplayPriorityLabel(String displayPriorityLabel) { this.displayPriorityLabel = displayPriorityLabel; }
        public String getRecommendedActionId() { return recommendedActionId; }
        public void setRecommendedActionId(String recommendedActionId) { this.recommendedActionId = recommendedActionId; }
        public String getDisplayColorToken() { return displayColorToken; }
        public void setDisplayColorToken(String displayColorToken) { this.displayColorToken = displayColorToken; }
    }

    public static class AdminRecommendedActionDto {
        private String id;
        private String title;
        private String type;
        private String severity;
        private String status;
        private Map<String, OperationalRecommendationDto.LocalizedContentDto> translations;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Map<String, OperationalRecommendationDto.LocalizedContentDto> getTranslations() { return translations; }
        public void setTranslations(Map<String, OperationalRecommendationDto.LocalizedContentDto> translations) { this.translations = translations; }
    }
}
