package com.itesm.application.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SystemDashboardSummaryDto {
    private LocalDateTime generatedAt;
    private List<SystemMetricDto> metrics = new ArrayList<>();
    private List<SystemActivityPointDto> userActivity = new ArrayList<>();
    private List<SystemRegionalDistributionDto> regionalDistribution = new ArrayList<>();
    private List<SystemEventDto> recentEvents = new ArrayList<>();

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
        private int value;

        public SystemActivityPointDto() {}

        public SystemActivityPointDto(String label, int value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
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
}
