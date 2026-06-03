package com.itesm.application.usecase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itesm.application.dto.OperationalRecommendationDto;
import com.itesm.application.dto.OperationalRecommendationDto.LocalizedContentDto;
import com.itesm.application.dto.OperationalRecommendationDto.NotificationDto;
import com.itesm.application.dto.OperationalRecommendationDto.NotificationRecipientDto;
import com.itesm.application.dto.OperationalRecommendationDto.RecipientSummaryDto;
import com.itesm.application.dto.OperationalRecommendationDto.RecommendationTargetDto;
import com.itesm.application.dto.OperationalRecommendationDto.TaskDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.HospitalDepartmentResource;
import com.itesm.domain.models.HospitalInventoryItem;
import com.itesm.domain.models.HospitalStaffingProfile;
import com.itesm.domain.models.OperationalRecommendation;
import com.itesm.domain.models.OperationalNotification;
import com.itesm.domain.models.OperationalNotificationRecipient;
import com.itesm.domain.models.OperationalTask;
import com.itesm.domain.models.User;
import com.itesm.domain.repository.HospitalResourceRepository;
import com.itesm.domain.repository.OperationalRecommendationRepository;
import com.itesm.domain.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ListOperationalRecommendationsUseCase {

    @Inject AuthenticatedUserContext authenticatedUserContext;
    @Inject OperationalRecommendationRepository repository;
    @Inject HospitalResourceRepository hospitalResourceRepository;
    @Inject UserRepository userRepository;
    @Inject RecommendationWorkflowPolicyService workflowPolicyService;
    @Inject OperationalRecommendationDedupeService dedupeService;
    @Inject ObjectMapper objectMapper;

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

        return dedupeService.collapseOpenDuplicates(recs).stream().map(this::toDto).collect(Collectors.toList());
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
        List<String> rationale = workflowPolicyService.parseStrings(r.getRationaleJson());
        List<String> recommendedActions = workflowPolicyService.parseStrings(r.getRecommendedActionsJson());
        dto.setRationale(rationale);
        dto.setRecommendedActions(recommendedActions);
        dto.setTranslations(resolveTranslations(r, rationale, recommendedActions));
        dto.setAffectedDepartments(workflowPolicyService.parseStrings(r.getAffectedDepartmentsJson()));
        dto.setAffectedResources(workflowPolicyService.parseStrings(r.getAffectedResourcesJson()));
        dto.setAllowedStatusTransitions(workflowPolicyService.parseStrings(r.getAllowedStatusTransitionsJson()));
        dto.setAvailableActions(workflowPolicyService.parseActions(r.getAvailableActionsJson()));
        dto.setPrimaryDepartment(resolveDepartmentTarget(r.getPrimaryDepartmentResourceId()));
        dto.setPrimaryStaffingProfile(resolveStaffingTarget(r.getPrimaryStaffingProfileId()));
        dto.setPrimaryInventoryItem(resolveInventoryTarget(r.getPrimaryInventoryItemId()));
        dto.setAssignedOwner(resolveOwnerTarget(r.getAssignedOwnerUserId()));
        dto.setTasks(repository.findTasksByRecommendationId(r.getId()).stream().map(this::taskToDto).collect(Collectors.toList()));
        dto.setNotifications(repository.findNotificationsByRecommendationId(r.getId()).stream().map(this::notificationToDto).collect(Collectors.toList()));
        return dto;
    }

    private TaskDto taskToDto(OperationalTask task) {
        TaskDto dto = new TaskDto();
        dto.setId(task.getId().toString());
        if (task.getOwnerContactId() != null) dto.setOwnerContactId(task.getOwnerContactId().toString());
        if (task.getOwnerGroupId() != null) dto.setOwnerGroupId(task.getOwnerGroupId().toString());
        dto.setOwnerLabel(task.getOwnerLabel());
        dto.setDepartmentLabel(task.getDepartmentLabel());
        dto.setPriority(task.getPriority());
        dto.setStatus(task.getStatus());
        dto.setSourceActionCode(task.getSourceActionCode());
        dto.setDeadlineAt(task.getDeadlineAt());
        dto.setNotes(task.getNotes());
        dto.setCreatedAt(task.getCreatedAt());
        return dto;
    }

    private NotificationDto notificationToDto(OperationalNotification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId().toString());
        if (notification.getAudienceGroupId() != null) dto.setAudienceGroupId(notification.getAudienceGroupId().toString());
        if (notification.getAudienceContactId() != null) dto.setAudienceContactId(notification.getAudienceContactId().toString());
        dto.setAudienceType(notification.getAudienceType());
        dto.setAudienceDepartmentCode(notification.getAudienceDepartmentCode());
        dto.setAudienceLabel(notification.getAudienceLabel());
        dto.setMessage(notification.getMessage());
        dto.setStatus(notification.getStatus());
        dto.setDeliveryChannel(notification.getDeliveryChannel());
        dto.setDeliveryStatusDetail(notification.getDeliveryStatusDetail());
        dto.setSourceActionCode(notification.getSourceActionCode());
        dto.setSentAt(notification.getSentAt());
        List<NotificationRecipientDto> recipients = notification.getRecipients().stream()
                .map(this::recipientToDto)
                .collect(Collectors.toList());
        dto.setRecipients(recipients);
        RecipientSummaryDto summary = new RecipientSummaryDto();
        summary.setTotal(recipients.size());
        summary.setSent((int) recipients.stream().filter(r -> "SENT".equalsIgnoreCase(r.getStatus())).count());
        summary.setFailed((int) recipients.stream().filter(r -> "FAILED".equalsIgnoreCase(r.getStatus())).count());
        dto.setRecipientSummary(summary);
        return dto;
    }

    private NotificationRecipientDto recipientToDto(OperationalNotificationRecipient recipient) {
        NotificationRecipientDto dto = new NotificationRecipientDto();
        dto.setId(recipient.getId().toString());
        if (recipient.getContactId() != null) dto.setContactId(recipient.getContactId().toString());
        dto.setRecipientName(recipient.getRecipientName());
        dto.setRecipientEmail(recipient.getRecipientEmail());
        dto.setStatus(recipient.getStatus());
        dto.setDeliveryStatusDetail(recipient.getDeliveryStatusDetail());
        dto.setDeliveredAt(recipient.getDeliveredAt());
        return dto;
    }

    private Map<String, LocalizedContentDto> resolveTranslations(
            OperationalRecommendation recommendation,
            List<String> rationale,
            List<String> recommendedActions) {
        Map<String, LocalizedContentDto> parsed = parseStoredTranslations(recommendation.getContentTranslationsJson());
        if (!parsed.isEmpty()) {
            return parsed;
        }

        Map<String, LocalizedContentDto> fallback = new HashMap<>();
        fallback.put("en", localized(
                recommendation.getTitle(),
                recommendation.getDescription(),
                recommendation.getExpectedImpact(),
                recommendation.getUrgencyWindow(),
                rationale,
                recommendedActions));
        fallback.put("es", localized(
                translateTitle(recommendation.getTitle()),
                translateDescription(recommendation),
                translateImpact(recommendation.getExpectedImpact()),
                translateUrgencyWindow(recommendation.getUrgencyWindow()),
                rationale,
                recommendedActions));
        return fallback;
    }

    private Map<String, LocalizedContentDto> parseStoredTranslations(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(rawJson, new TypeReference<Map<String, LocalizedContentDto>>() {});
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private LocalizedContentDto localized(
            String title,
            String description,
            String expectedImpact,
            String urgencyWindow,
            List<String> rationale,
            List<String> recommendedActions) {
        LocalizedContentDto dto = new LocalizedContentDto();
        dto.setTitle(title);
        dto.setDescription(description);
        dto.setExpectedImpact(expectedImpact);
        dto.setUrgencyWindow(urgencyWindow);
        dto.setRationale(rationale);
        dto.setRecommendedActions(recommendedActions);
        return dto;
    }

    private String translateTitle(String title) {
        if ("Expand Monitored Bed Capacity".equals(title)) return "Expandir capacidad de camas monitoreadas";
        if ("Monitor Bed Occupancy Trend".equals(title)) return "Monitorear tendencia de ocupacion de camas";
        if ("Increase Emergency Physician Staffing".equals(title)) return "Aumentar cobertura de medicos de urgencias";
        if ("ICU Capacity Critical - Activate Surge Protocol".equals(title)) return "Activar protocolo de expansion UCI";
        if ("Implement Respiratory Isolation Measures".equals(title)) return "Implementar medidas de aislamiento respiratorio";
        if ("Replenish Critical Protective and Respiratory Supplies".equals(title)) return "Reabastecer insumos criticos de proteccion respiratoria";
        if ("Review PPE Stock Levels".equals(title)) return "Revisar niveles de inventario de EPP";
        return title;
    }

    private String translateDescription(OperationalRecommendation recommendation) {
        String title = recommendation.getTitle();
        if ("Expand Monitored Bed Capacity".equals(title)) {
            return "Abrir camas monitoreadas adicionales para prevenir saturacion de capacidad hospitalaria.";
        }
        if ("Monitor Bed Occupancy Trend".equals(title)) {
            return "Iniciar planeacion de contingencia para evitar llegar a capacidad critica.";
        }
        if ("Increase Emergency Physician Staffing".equals(title)) {
            return "Aumentar cobertura medica de urgencias ante presion por brotes cercanos y demanda hospitalaria.";
        }
        if ("ICU Capacity Critical - Activate Surge Protocol".equals(title)) {
            return "La ocupacion UCI esta en nivel critico; activar el protocolo de expansion para preservar cuidados intensivos.";
        }
        if ("Implement Respiratory Isolation Measures".equals(title)) {
            return "Establecer zonas de aislamiento respiratorio para reducir transmision intrahospitalaria durante el brote activo.";
        }
        if ("Replenish Critical Protective and Respiratory Supplies".equals(title)) {
            return "Reabastecer insumos criticos de proteccion y respuesta respiratoria para mantener seguridad del personal y atencion continua.";
        }
        if ("Review PPE Stock Levels".equals(title)) {
            return "Revisar inventario de EPP y preparar reposicion preventiva ante aumento de consumo.";
        }
        return recommendation.getDescription();
    }

    private String translateImpact(String expectedImpact) {
        if ("Reduce patient wait times and prevent diversion".equals(expectedImpact)) {
            return "Reducir tiempos de espera y prevenir derivacion de pacientes";
        }
        if ("Prevent critical bed shortage".equals(expectedImpact)) {
            return "Prevenir escasez critica de camas";
        }
        if ("Improve patient throughput during outbreak surge".equals(expectedImpact)) {
            return "Mejorar flujo de pacientes durante aumento por brote";
        }
        if ("Prevent ICU overflow and ensure critical care availability".equals(expectedImpact)) {
            return "Prevenir saturacion UCI y asegurar disponibilidad de cuidados criticos";
        }
        if ("Reduce risk of influenza transmission to staff and patients within the hospital".equals(expectedImpact)) {
            return "Reducir riesgo de transmision respiratoria a personal y pacientes dentro del hospital";
        }
        if ("Ensure uninterrupted staff protection and maintain readiness for respiratory surge".equals(expectedImpact)) {
            return "Asegurar proteccion continua del personal y preparacion ante demanda respiratoria";
        }
        if ("Avoid PPE stockout during active outbreak period".equals(expectedImpact)) {
            return "Evitar agotamiento de EPP durante el periodo de brote activo";
        }
        return expectedImpact;
    }

    private String translateUrgencyWindow(String urgencyWindow) {
        if ("Immediately".equals(urgencyWindow)) return "Inmediatamente";
        if ("Within 12 hours".equals(urgencyWindow)) return "Dentro de 12 horas";
        if ("Within 24 hours".equals(urgencyWindow)) return "Dentro de 24 horas";
        if ("Within 48 hours".equals(urgencyWindow)) return "Dentro de 48 horas";
        return urgencyWindow;
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
