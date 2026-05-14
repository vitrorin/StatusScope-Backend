package com.itesm.application.usecase;

import com.itesm.application.dto.OperationalRecommendationDto;
import com.itesm.application.dto.OperationalRecommendationDto.RecommendationTargetDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.HospitalDepartmentResource;
import com.itesm.domain.models.HospitalInventoryItem;
import com.itesm.domain.models.HospitalStaffingProfile;
import com.itesm.domain.models.OperationalRecommendation;
import com.itesm.domain.models.User;
import com.itesm.domain.repository.HospitalResourceRepository;
import com.itesm.domain.repository.OperationalRecommendationRepository;
import com.itesm.domain.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ListOperationalRecommendationsUseCase {

    @Inject AuthenticatedUserContext authenticatedUserContext;
    @Inject OperationalRecommendationRepository repository;
    @Inject HospitalResourceRepository hospitalResourceRepository;
    @Inject UserRepository userRepository;
    @Inject RecommendationWorkflowPolicyService workflowPolicyService;

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
        workflowPolicyService.populateDefaults(
                r,
                hospitalResourceRepository.findDepartmentsByHospitalId(r.getHospitalId()),
                hospitalResourceRepository.findStaffingByHospitalId(r.getHospitalId()),
                hospitalResourceRepository.findInventoryByHospitalId(r.getHospitalId()));

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
        dto.setDisplayCategoryLabel(r.getDisplayCategoryLabel());
        dto.setDisplaySeverityLabel(r.getDisplaySeverityLabel());
        dto.setDisplayStatusLabel(r.getDisplayStatusLabel());
        dto.setPrimaryActionCode(r.getPrimaryActionCode());
        dto.setExpiresAt(r.getExpiresAt());
        dto.setCreatedByMode(r.getCreatedByMode());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setUpdatedAt(r.getUpdatedAt());
        dto.setResolvedAt(r.getResolvedAt());
        dto.setRationale(workflowPolicyService.parseStrings(r.getRationaleJson()));
        dto.setRecommendedActions(workflowPolicyService.parseStrings(r.getRecommendedActionsJson()));
        dto.setAffectedDepartments(workflowPolicyService.parseStrings(r.getAffectedDepartmentsJson()));
        dto.setAffectedResources(workflowPolicyService.parseStrings(r.getAffectedResourcesJson()));
        dto.setAllowedStatusTransitions(workflowPolicyService.parseStrings(r.getAllowedStatusTransitionsJson()));
        dto.setAvailableActions(workflowPolicyService.parseActions(r.getAvailableActionsJson()));
        dto.setPrimaryDepartment(resolveDepartmentTarget(r.getPrimaryDepartmentResourceId()));
        dto.setPrimaryStaffingProfile(resolveStaffingTarget(r.getPrimaryStaffingProfileId()));
        dto.setPrimaryInventoryItem(resolveInventoryTarget(r.getPrimaryInventoryItemId()));
        dto.setAssignedOwner(resolveOwnerTarget(r.getAssignedOwnerUserId()));
        return dto;
    }

    private RecommendationTargetDto resolveDepartmentTarget(UUID departmentId) {
        if (departmentId == null) {
            return null;
        }
        HospitalDepartmentResource department = hospitalResourceRepository.findDepartmentById(departmentId).orElse(null);
        if (department == null) {
            return null;
        }
        RecommendationTargetDto dto = new RecommendationTargetDto();
        dto.setId(department.getId().toString());
        dto.setLabel(department.getDepartmentName());
        dto.setType("DEPARTMENT");
        return dto;
    }

    private RecommendationTargetDto resolveStaffingTarget(UUID staffingId) {
        if (staffingId == null) {
            return null;
        }
        HospitalStaffingProfile profile = hospitalResourceRepository.findStaffingProfileById(staffingId).orElse(null);
        if (profile == null) {
            return null;
        }
        RecommendationTargetDto dto = new RecommendationTargetDto();
        dto.setId(profile.getId().toString());
        dto.setLabel(profile.getRoleName());
        dto.setType("STAFFING_PROFILE");
        return dto;
    }

    private RecommendationTargetDto resolveInventoryTarget(UUID inventoryId) {
        if (inventoryId == null) {
            return null;
        }
        HospitalInventoryItem item = hospitalResourceRepository.findInventoryItemById(inventoryId).orElse(null);
        if (item == null) {
            return null;
        }
        RecommendationTargetDto dto = new RecommendationTargetDto();
        dto.setId(item.getId().toString());
        dto.setLabel(item.getItemName());
        dto.setType("INVENTORY_ITEM");
        return dto;
    }

    private RecommendationTargetDto resolveOwnerTarget(UUID userId) {
        if (userId == null) {
            return null;
        }
        User user = userRepository.findUserById(userId).orElse(null);
        if (user == null) {
            return null;
        }
        RecommendationTargetDto dto = new RecommendationTargetDto();
        dto.setId(user.getId().toString());
        dto.setLabel(user.getFullName());
        dto.setType("USER");
        return dto;
    }
}
