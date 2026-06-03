package com.itesm.application.usecase;

import com.itesm.domain.models.OperationalRecommendation;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class OperationalRecommendationDedupeService {

    public List<OperationalRecommendation> collapseOpenDuplicates(List<OperationalRecommendation> recommendations) {
        Map<String, OperationalRecommendation> selected = new LinkedHashMap<>();
        for (OperationalRecommendation recommendation : recommendations) {
            String key = isOpen(recommendation)
                    ? semanticKey(recommendation)
                    : "closed:" + recommendation.getId();
            OperationalRecommendation current = selected.get(key);
            if (current == null || isPreferred(recommendation, current)) {
                selected.put(key, recommendation);
            }
        }
        return List.copyOf(selected.values());
    }

    public Optional<OperationalRecommendation> findOpenDuplicate(
            List<OperationalRecommendation> existingRecommendations,
            OperationalRecommendation candidate) {
        String candidateKey = semanticKey(candidate);
        return existingRecommendations.stream()
                .filter(this::isOpen)
                .filter(existing -> candidateKey.equals(semanticKey(existing)))
                .reduce((best, current) -> isPreferred(current, best) ? current : best);
    }

    public Optional<OperationalRecommendation> findRefreshDuplicate(
            List<OperationalRecommendation> existingRecommendations,
            OperationalRecommendation candidate) {
        String candidateKey = semanticKey(candidate);
        return existingRecommendations.stream()
                .filter(existing -> candidateKey.equals(semanticKey(existing)))
                .reduce((best, current) -> isPreferred(current, best) ? current : best);
    }

    private String semanticKey(OperationalRecommendation recommendation) {
        UUID hospitalId = recommendation.getHospitalId();
        String type = normalize(recommendation.getType());
        String text = normalize(
                safe(recommendation.getTitle()) + " " +
                        safe(recommendation.getDescription()) + " " +
                        safe(recommendation.getExpectedImpact()));

        if (type.startsWith("EPIDEMIOLOGY")) {
            String scope = normalize(extractJsonValue(recommendation.getInputContextJson(), "scope"));
            String diseaseName = normalize(extractJsonValue(recommendation.getInputContextJson(), "diseaseName"));
            if (!scope.isBlank() && !diseaseName.isBlank()) {
                return hospitalId + ":" + type + ":" + scope + ":" + diseaseName;
            }
            return hospitalId + ":" + type + ":" + normalize(recommendation.getTitle());
        }

        if ("BED_CAPACITY".equals(type)) {
            if (text.contains("ICU") || text.contains("SURGE PROTOCOL") || text.contains("CRITICAL CARE")) {
                return hospitalId + ":BED_CAPACITY:ICU_SURGE";
            }
            if (text.contains("BED CAPACITY") || text.contains("MONITORED BED") || text.contains("BED OCCUPANCY")) {
                return hospitalId + ":BED_CAPACITY:EXPAND_BEDS";
            }
        }
        if ("SUPPLY".equals(type) && (text.contains("PROTECTIVE") || text.contains("RESPIRATORY SUPPL")
                || text.contains("PPE") || text.contains("REPLENISH"))) {
            return hospitalId + ":SUPPLY:RESPIRATORY_SUPPLIES";
        }
        if (("ISOLATION".equals(type) || "LOCAL_EPIDEMIOLOGY".equals(type))
                && (text.contains("RESPIRATORY") || text.contains("ISOLATION") || text.contains("EPIDEMIOLOGY"))) {
            return hospitalId + ":LOCAL_EPIDEMIOLOGY:RESPIRATORY_READINESS";
        }
        if ("STAFFING".equals(type) && (text.contains("EMERGENCY PHYSICIAN") || text.contains("STAFFING"))) {
            return hospitalId + ":STAFFING:EMERGENCY_PHYSICIANS";
        }

        return hospitalId + ":" + type + ":" + normalize(recommendation.getTitle());
    }

    private boolean isOpen(OperationalRecommendation recommendation) {
        String status = normalize(recommendation.getStatus());
        return !"COMPLETED".equals(status) && !"REJECTED".equals(status);
    }

    private boolean isPreferred(OperationalRecommendation candidate, OperationalRecommendation current) {
        int candidateStatus = statusRank(candidate.getStatus());
        int currentStatus = statusRank(current.getStatus());
        if (candidateStatus != currentStatus) {
            return candidateStatus > currentStatus;
        }
        return timestamp(candidate).isAfter(timestamp(current));
    }

    private int statusRank(String status) {
        return switch (normalize(status)) {
            case "IN_PROGRESS" -> 4;
            case "ASSIGNED" -> 3;
            case "ACCEPTED" -> 2;
            case "NEW" -> 1;
            case "COMPLETED", "REJECTED" -> 5;
            default -> 0;
        };
    }

    private LocalDateTime timestamp(OperationalRecommendation recommendation) {
        if (recommendation.getUpdatedAt() != null) {
            return recommendation.getUpdatedAt();
        }
        if (recommendation.getCreatedAt() != null) {
            return recommendation.getCreatedAt();
        }
        return LocalDateTime.MIN;
    }

    private String normalize(String value) {
        return safe(value).toUpperCase().replaceAll("[^A-Z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String extractJsonValue(String json, String fieldName) {
        if (json == null || json.isBlank()) {
            return "";
        }
        String needle = "\"" + fieldName + "\"";
        int fieldIndex = json.indexOf(needle);
        if (fieldIndex < 0) {
            return "";
        }
        int colonIndex = json.indexOf(':', fieldIndex + needle.length());
        if (colonIndex < 0) {
            return "";
        }
        int firstQuote = json.indexOf('"', colonIndex + 1);
        if (firstQuote < 0) {
            return "";
        }
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) {
            return "";
        }
        return json.substring(firstQuote + 1, secondQuote);
    }
}
