package com.itesm.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itesm.application.dto.OperationalRecommendationDto.RecommendationActionDto;
import com.itesm.domain.models.HospitalDepartmentResource;
import com.itesm.domain.models.HospitalInventoryItem;
import com.itesm.domain.models.HospitalStaffingProfile;
import com.itesm.domain.models.OperationalRecommendation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class RecommendationWorkflowPolicyService {

    @Inject ObjectMapper objectMapper;

    public void populateDefaults(
            OperationalRecommendation recommendation,
            List<HospitalDepartmentResource> departments,
            List<HospitalStaffingProfile> staffingProfiles,
            List<HospitalInventoryItem> inventoryItems) {
        if (recommendation.getPrimaryDepartmentResourceId() == null) {
            HospitalDepartmentResource department = switch (recommendation.getType()) {
                case "BED_CAPACITY" -> firstDepartment(departments, "ICU", "GENERAL");
                case "STAFFING" -> firstDepartment(departments, "EMERGENCY", "ICU");
                case "ISOLATION", "LOCAL_EPIDEMIOLOGY" -> firstDepartment(departments, "RESPIRATORY", "EMERGENCY");
                case "SUPPLY" -> firstDepartment(departments, "EMERGENCY", "RESPIRATORY");
                default -> null;
            };
            if (department != null) {
                recommendation.setPrimaryDepartmentResourceId(department.getId());
            }
        }

        if (recommendation.getPrimaryStaffingProfileId() == null) {
            HospitalStaffingProfile staffing = switch (recommendation.getType()) {
                case "BED_CAPACITY" -> firstStaffing(staffingProfiles, "ICU_NURSE", "NURSING_STAFF");
                case "STAFFING" -> firstStaffing(staffingProfiles, "EMERGENCY_PHYSICIAN", "GENERAL_PRACTITIONER");
                case "ISOLATION", "LOCAL_EPIDEMIOLOGY" -> firstStaffing(staffingProfiles, "INFECTIOUS_DISEASE", "PULMONOLOGIST");
                default -> null;
            };
            if (staffing != null) {
                recommendation.setPrimaryStaffingProfileId(staffing.getId());
            }
        }

        if (recommendation.getPrimaryInventoryItemId() == null) {
            HospitalInventoryItem inventory = switch (recommendation.getType()) {
                case "SUPPLY" -> firstInventory(inventoryItems, "ISO_GOWN", "N95_MASK", "O2_CYLINDER");
                case "ISOLATION", "LOCAL_EPIDEMIOLOGY" -> firstInventory(inventoryItems, "N95_MASK", "ISO_GOWN");
                case "BED_CAPACITY" -> firstInventory(inventoryItems, "VENTILATOR", "O2_CYLINDER");
                default -> null;
            };
            if (inventory != null) {
                recommendation.setPrimaryInventoryItemId(inventory.getId());
            }
        }

        if (recommendation.getPresentationVariant() == null) {
            recommendation.setPresentationVariant(switch (recommendation.getSeverity()) {
                case "CRITICAL" -> "alert";
                case "HIGH" -> "urgent";
                default -> "standard";
            });
        }
        recommendation.setPrimaryActionCode(resolvePrimaryActionCode(recommendation));
        recommendation.setAvailableActionsJson(serializeActions(buildActionMaps(recommendation)));
        recommendation.setAllowedStatusTransitionsJson(serializeStrings(resolveAllowedStatusTransitions(recommendation.getStatus())));
        if (recommendation.getDisplayCategoryLabel() == null || recommendation.getDisplayCategoryLabel().isBlank()) {
            recommendation.setDisplayCategoryLabel(toDisplayLabel(recommendation.getCategory() != null ? recommendation.getCategory() : recommendation.getType()));
        }
        if (recommendation.getDisplaySeverityLabel() == null || recommendation.getDisplaySeverityLabel().isBlank()) {
            recommendation.setDisplaySeverityLabel(toDisplayLabel(recommendation.getSeverity()));
        }
        recommendation.setDisplayStatusLabel(toDisplayLabel(recommendation.getStatus()));
        if (recommendation.getExpiresAt() == null) {
            recommendation.setExpiresAt(resolveExpiry(recommendation));
        }
    }

    public List<RecommendationActionDto> parseActions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(json, new TypeReference<>() {});
            List<RecommendationActionDto> actions = new ArrayList<>();
            for (Map<String, Object> item : raw) {
                RecommendationActionDto dto = new RecommendationActionDto();
                dto.setCode((String) item.get("code"));
                dto.setLabel((String) item.get("label"));
                dto.setStyle((String) item.get("style"));
                dto.setEnabled(Boolean.TRUE.equals(item.get("enabled")));
                dto.setDisabledReason((String) item.get("disabledReason"));
                actions.add(dto);
            }
            return actions;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse recommendation actions JSON", e);
        }
    }

    public List<String> parseStrings(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse recommendation string list JSON", e);
        }
    }

    public String resolvePrimaryActionCode(OperationalRecommendation recommendation) {
        return switch (recommendation.getType()) {
            case "SUPPLY" -> "ORDER_SUPPLIES";
            case "STAFFING", "BED_CAPACITY" -> "ASSIGN_TASK";
            case "ISOLATION", "LOCAL_EPIDEMIOLOGY" -> "NOTIFY_STAFF";
            default -> "ASSIGN_TASK";
        };
    }

    public List<String> resolveAllowedStatusTransitions(String status) {
        return switch (status != null ? status : "NEW") {
            case "NEW" -> List.of("ACCEPTED", "ASSIGNED", "REJECTED");
            case "ACCEPTED" -> List.of("ASSIGNED", "COMPLETED", "REJECTED");
            case "ASSIGNED" -> List.of("IN_PROGRESS", "COMPLETED", "REJECTED");
            case "IN_PROGRESS" -> List.of("COMPLETED", "REJECTED");
            default -> List.of();
        };
    }

    private LocalDateTime resolveExpiry(OperationalRecommendation recommendation) {
        LocalDateTime base = recommendation.getCreatedAt() != null ? recommendation.getCreatedAt() : LocalDateTime.now();
        return switch (recommendation.getSeverity() != null ? recommendation.getSeverity() : "MEDIUM") {
            case "CRITICAL" -> base.plusHours(12);
            case "HIGH" -> base.plusDays(1);
            default -> base.plusDays(3);
        };
    }

    private List<Map<String, Object>> buildActionMaps(OperationalRecommendation recommendation) {
        boolean closed = List.of("COMPLETED", "REJECTED").contains(recommendation.getStatus());
        boolean hasInventory = recommendation.getPrimaryInventoryItemId() != null;
        List<Map<String, Object>> actions = new ArrayList<>();
        actions.add(action(
                "ASSIGN_TASK",
                "Assign task",
                "ASSIGN_TASK".equals(resolvePrimaryActionCode(recommendation)) ? "primary" : "secondary",
                !closed,
                closed ? "Recommendation is already closed" : null));
        actions.add(action(
                "NOTIFY_STAFF",
                "Notify staff",
                "NOTIFY_STAFF".equals(resolvePrimaryActionCode(recommendation)) ? "primary" : "secondary",
                !closed,
                closed ? "Recommendation is already closed" : null));
        actions.add(action(
                "ORDER_SUPPLIES",
                "Order supplies",
                "ORDER_SUPPLIES".equals(resolvePrimaryActionCode(recommendation)) ? "primary" : "secondary",
                !closed && hasInventory,
                closed ? "Recommendation is already closed" : hasInventory ? null : "No inventory item linked"));
        return actions;
    }

    private Map<String, Object> action(String code, String label, String style, boolean enabled, String disabledReason) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("code", code);
        item.put("label", label);
        item.put("style", style);
        item.put("enabled", enabled);
        if (disabledReason != null) {
            item.put("disabledReason", disabledReason);
        }
        return item;
    }

    private String serializeActions(List<Map<String, Object>> actions) {
        try {
            return objectMapper.writeValueAsString(actions);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize recommendation actions", e);
        }
    }

    private String serializeStrings(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize recommendation transitions", e);
        }
    }

    private String toDisplayLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String[] parts = raw.toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        return builder.toString();
    }

    private HospitalDepartmentResource firstDepartment(List<HospitalDepartmentResource> departments, String... codes) {
        for (String code : codes) {
            for (HospitalDepartmentResource department : departments) {
                if (code.equalsIgnoreCase(department.getDepartmentCode())) {
                    return department;
                }
            }
        }
        return departments.isEmpty() ? null : departments.get(0);
    }

    private HospitalStaffingProfile firstStaffing(List<HospitalStaffingProfile> profiles, String... codes) {
        for (String code : codes) {
            for (HospitalStaffingProfile profile : profiles) {
                if (code.equalsIgnoreCase(profile.getRoleCode())) {
                    return profile;
                }
            }
        }
        return profiles.isEmpty() ? null : profiles.get(0);
    }

    private HospitalInventoryItem firstInventory(List<HospitalInventoryItem> items, String... codes) {
        for (String code : codes) {
            for (HospitalInventoryItem item : items) {
                if (code.equalsIgnoreCase(item.getItemCode())) {
                    return item;
                }
            }
        }
        return items.isEmpty() ? null : items.get(0);
    }
}
