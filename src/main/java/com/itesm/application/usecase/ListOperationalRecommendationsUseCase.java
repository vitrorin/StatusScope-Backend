package com.itesm.application.usecase;

import com.itesm.application.dto.OperationalRecommendationDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.OperationalRecommendation;
import com.itesm.domain.repository.OperationalRecommendationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ListOperationalRecommendationsUseCase {

    @Inject AuthenticatedUserContext authenticatedUserContext;
    @Inject OperationalRecommendationRepository repository;

    public List<OperationalRecommendationDto> execute(String status, String severity, String type) {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        UUID hospitalId = currentUser.getHospitalId();
        if (hospitalId == null) throw new NotFoundException("User has no assigned hospital");

        List<OperationalRecommendation> recs;
        if (status != null && !status.isBlank()) {
            recs = repository.findByHospitalIdAndStatus(hospitalId, status.toUpperCase());
        } else if (severity != null && !severity.isBlank()) {
            recs = repository.findByHospitalIdAndSeverity(hospitalId, severity.toUpperCase());
        } else {
            recs = repository.findByHospitalId(hospitalId);
        }

        if (type != null && !type.isBlank()) {
            String typeUpper = type.toUpperCase();
            recs = recs.stream().filter(r -> typeUpper.equals(r.getType())).collect(Collectors.toList());
        }

        return recs.stream().map(this::toDto).collect(Collectors.toList());
    }

    OperationalRecommendationDto toDto(OperationalRecommendation r) {
        OperationalRecommendationDto dto = new OperationalRecommendationDto();
        dto.setId(r.getId().toString());
        dto.setHospitalId(r.getHospitalId().toString());
        if (r.getSourceAlertId() != null) dto.setSourceAlertId(r.getSourceAlertId().toString());
        if (r.getSourceOutbreakId() != null) dto.setSourceOutbreakId(r.getSourceOutbreakId().toString());
        dto.setType(r.getType());
        dto.setSeverity(r.getSeverity());
        dto.setStatus(r.getStatus());
        dto.setCategory(r.getCategory());
        dto.setTitle(r.getTitle());
        dto.setDescription(r.getDescription());
        dto.setExpectedImpact(r.getExpectedImpact());
        dto.setUrgencyWindow(r.getUrgencyWindow());
        dto.setConfidenceScore(r.getConfidenceScore());
        dto.setImageMode(r.getImageMode());
        dto.setCreatedByMode(r.getCreatedByMode());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setUpdatedAt(r.getUpdatedAt());
        dto.setResolvedAt(r.getResolvedAt());
        dto.setRationale(parseJsonArray(r.getRationaleJson()));
        dto.setRecommendedActions(parseJsonArray(r.getRecommendedActionsJson()));
        dto.setAffectedDepartments(parseJsonArray(r.getAffectedDepartmentsJson()));
        dto.setAffectedResources(parseJsonArray(r.getAffectedResourcesJson()));
        return dto;
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return List.of();
        // Simple extraction of string values from a JSON array like ["a","b"]
        String stripped = json.trim();
        if (!stripped.startsWith("[")) return List.of(stripped);
        stripped = stripped.substring(1, stripped.length() - 1).trim();
        if (stripped.isEmpty()) return List.of();
        return java.util.Arrays.stream(stripped.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"))
                .map(s -> s.trim().replaceAll("^\"|\"$", ""))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
