package com.itesm.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class DoctorDashboardSummaryDto {
    private String hospitalName;
    private String municipalityName;
    private String stateName;
    private double radiusKm;
    private LocalDateTime generatedAt;
    private List<DoctorDashboardMetricDto> metrics;
    private List<DoctorDashboardDiseaseDto> diseaseBreakdown;
    private List<DoctorDashboardDiseaseDto> stateDiseaseBreakdown;
    private List<DoctorDashboardAlertDto> alerts;
    private List<DoctorDashboardZoneDto> zones;

    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }

    public String getMunicipalityName() { return municipalityName; }
    public void setMunicipalityName(String municipalityName) { this.municipalityName = municipalityName; }

    public String getStateName() { return stateName; }
    public void setStateName(String stateName) { this.stateName = stateName; }

    public double getRadiusKm() { return radiusKm; }
    public void setRadiusKm(double radiusKm) { this.radiusKm = radiusKm; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public List<DoctorDashboardMetricDto> getMetrics() { return metrics; }
    public void setMetrics(List<DoctorDashboardMetricDto> metrics) { this.metrics = metrics; }

    public List<DoctorDashboardDiseaseDto> getDiseaseBreakdown() { return diseaseBreakdown; }
    public void setDiseaseBreakdown(List<DoctorDashboardDiseaseDto> diseaseBreakdown) { this.diseaseBreakdown = diseaseBreakdown; }

    public List<DoctorDashboardDiseaseDto> getStateDiseaseBreakdown() { return stateDiseaseBreakdown; }
    public void setStateDiseaseBreakdown(List<DoctorDashboardDiseaseDto> stateDiseaseBreakdown) { this.stateDiseaseBreakdown = stateDiseaseBreakdown; }

    public List<DoctorDashboardAlertDto> getAlerts() { return alerts; }
    public void setAlerts(List<DoctorDashboardAlertDto> alerts) { this.alerts = alerts; }

    public List<DoctorDashboardZoneDto> getZones() { return zones; }
    public void setZones(List<DoctorDashboardZoneDto> zones) { this.zones = zones; }

    public static class DoctorDashboardMetricDto {
        private String id;
        private String title;
        private String value;
        private String badge;
        private String status;
        private String subtitle;
        private String detailSummary;
        private String signalLabel;
        private String recommendedAction;
        private String iconKey;
        private List<DoctorDashboardMetricInsightDto> insights;

        public DoctorDashboardMetricDto() {}

        public DoctorDashboardMetricDto(String id, String title, String value, String badge, String status,
                                        String subtitle, String detailSummary, String signalLabel,
                                        String recommendedAction, String iconKey) {
            this(id, title, value, badge, status, subtitle, detailSummary, signalLabel, recommendedAction, iconKey, List.of());
        }

        public DoctorDashboardMetricDto(String id, String title, String value, String badge, String status,
                                        String subtitle, String detailSummary, String signalLabel,
                                        String recommendedAction, String iconKey,
                                        List<DoctorDashboardMetricInsightDto> insights) {
            this.id = id;
            this.title = title;
            this.value = value;
            this.badge = badge;
            this.status = status;
            this.subtitle = subtitle;
            this.detailSummary = detailSummary;
            this.signalLabel = signalLabel;
            this.recommendedAction = recommendedAction;
            this.iconKey = iconKey;
            this.insights = insights;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getBadge() { return badge; }
        public void setBadge(String badge) { this.badge = badge; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getSubtitle() { return subtitle; }
        public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
        public String getDetailSummary() { return detailSummary; }
        public void setDetailSummary(String detailSummary) { this.detailSummary = detailSummary; }
        public String getSignalLabel() { return signalLabel; }
        public void setSignalLabel(String signalLabel) { this.signalLabel = signalLabel; }
        public String getRecommendedAction() { return recommendedAction; }
        public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
        public String getIconKey() { return iconKey; }
        public void setIconKey(String iconKey) { this.iconKey = iconKey; }
        public List<DoctorDashboardMetricInsightDto> getInsights() { return insights; }
        public void setInsights(List<DoctorDashboardMetricInsightDto> insights) { this.insights = insights; }
    }

    public static class DoctorDashboardMetricInsightDto {
        private String title;
        private String location;
        private String cases;
        private String severity;
        private String color;
        private String meta;

        public DoctorDashboardMetricInsightDto() {}

        public DoctorDashboardMetricInsightDto(String title, String location, String cases, String severity, String color, String meta) {
            this.title = title;
            this.location = location;
            this.cases = cases;
            this.severity = severity;
            this.color = color;
            this.meta = meta;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getCases() { return cases; }
        public void setCases(String cases) { this.cases = cases; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public String getMeta() { return meta; }
        public void setMeta(String meta) { this.meta = meta; }
    }

    public static class DoctorDashboardDiseaseDto {
        private String diseaseName;
        private int caseCount;
        private int outbreakCount;
        private int progress;

        public DoctorDashboardDiseaseDto() {}

        public DoctorDashboardDiseaseDto(String diseaseName, int caseCount, int outbreakCount, int progress) {
            this.diseaseName = diseaseName;
            this.caseCount = caseCount;
            this.outbreakCount = outbreakCount;
            this.progress = progress;
        }

        public String getDiseaseName() { return diseaseName; }
        public void setDiseaseName(String diseaseName) { this.diseaseName = diseaseName; }
        public int getCaseCount() { return caseCount; }
        public void setCaseCount(int caseCount) { this.caseCount = caseCount; }
        public int getOutbreakCount() { return outbreakCount; }
        public void setOutbreakCount(int outbreakCount) { this.outbreakCount = outbreakCount; }
        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }
    }

    public static class DoctorDashboardAlertDto {
        private String id;
        private String title;
        private String description;
        private String variant;
        private String area;
        private String priority;
        private String recommendedAction;
        private int caseCount;
        private String caseLabel;
        private String confirmationStatus;
        private String municipalityName;
        private String stateName;

        public DoctorDashboardAlertDto() {}

        public DoctorDashboardAlertDto(String id, String title, String description, String variant,
                                       String area, String priority, String recommendedAction) {
            this(id, title, description, variant, area, priority, recommendedAction, 0, null, null, null, null);
        }

        public DoctorDashboardAlertDto(String id, String title, String description, String variant,
                                       String area, String priority, String recommendedAction,
                                       int caseCount, String caseLabel, String confirmationStatus,
                                       String municipalityName, String stateName) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.variant = variant;
            this.area = area;
            this.priority = priority;
            this.recommendedAction = recommendedAction;
            this.caseCount = caseCount;
            this.caseLabel = caseLabel;
            this.confirmationStatus = confirmationStatus;
            this.municipalityName = municipalityName;
            this.stateName = stateName;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getVariant() { return variant; }
        public void setVariant(String variant) { this.variant = variant; }
        public String getArea() { return area; }
        public void setArea(String area) { this.area = area; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getRecommendedAction() { return recommendedAction; }
        public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
        public int getCaseCount() { return caseCount; }
        public void setCaseCount(int caseCount) { this.caseCount = caseCount; }
        public String getCaseLabel() { return caseLabel; }
        public void setCaseLabel(String caseLabel) { this.caseLabel = caseLabel; }
        public String getConfirmationStatus() { return confirmationStatus; }
        public void setConfirmationStatus(String confirmationStatus) { this.confirmationStatus = confirmationStatus; }
        public String getMunicipalityName() { return municipalityName; }
        public void setMunicipalityName(String municipalityName) { this.municipalityName = municipalityName; }
        public String getStateName() { return stateName; }
        public void setStateName(String stateName) { this.stateName = stateName; }
    }

    public static class DoctorDashboardZoneDto {
        private String id;
        private String name;
        private String risk;
        private String disease;
        private String cases;
        private String radius;
        private String priority;
        private String note;
        private String recommendedAction;
        private String municipalityName;
        private String stateName;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String borderColor;

        public DoctorDashboardZoneDto() {}

        public DoctorDashboardZoneDto(String id, String name, String risk, String disease, String cases,
                                      String radius, String priority, String note, String recommendedAction,
                                      String municipalityName, String stateName,
                                      BigDecimal latitude, BigDecimal longitude, String borderColor) {
            this.id = id;
            this.name = name;
            this.risk = risk;
            this.disease = disease;
            this.cases = cases;
            this.radius = radius;
            this.priority = priority;
            this.note = note;
            this.recommendedAction = recommendedAction;
            this.municipalityName = municipalityName;
            this.stateName = stateName;
            this.latitude = latitude;
            this.longitude = longitude;
            this.borderColor = borderColor;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRisk() { return risk; }
        public void setRisk(String risk) { this.risk = risk; }
        public String getDisease() { return disease; }
        public void setDisease(String disease) { this.disease = disease; }
        public String getCases() { return cases; }
        public void setCases(String cases) { this.cases = cases; }
        public String getRadius() { return radius; }
        public void setRadius(String radius) { this.radius = radius; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
        public String getRecommendedAction() { return recommendedAction; }
        public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
        public String getMunicipalityName() { return municipalityName; }
        public void setMunicipalityName(String municipalityName) { this.municipalityName = municipalityName; }
        public String getStateName() { return stateName; }
        public void setStateName(String stateName) { this.stateName = stateName; }
        public BigDecimal getLatitude() { return latitude; }
        public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
        public BigDecimal getLongitude() { return longitude; }
        public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
        public String getBorderColor() { return borderColor; }
        public void setBorderColor(String borderColor) { this.borderColor = borderColor; }
    }
}
