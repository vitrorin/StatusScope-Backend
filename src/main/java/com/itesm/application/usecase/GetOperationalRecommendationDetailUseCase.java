package com.itesm.application.usecase;

import com.itesm.application.dto.OperationalRecommendationDto;
import com.itesm.application.dto.OperationalRecommendationDto.AuditEntryDto;
import com.itesm.application.dto.OperationalRecommendationDto.TaskDto;
import com.itesm.application.dto.OperationalRecommendationDto.NotificationDto;
import com.itesm.application.dto.OperationalRecommendationDto.SupplyRequestItemDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.OperationalRecommendation;
import com.itesm.domain.repository.OperationalRecommendationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class GetOperationalRecommendationDetailUseCase {

    @Inject AuthenticatedUserContext authenticatedUserContext;
    @Inject OperationalRecommendationRepository repository;
    @Inject ListOperationalRecommendationsUseCase listUseCase;

    public OperationalRecommendationDto execute(UUID recommendationId) {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        UUID hospitalId = currentUser.getHospitalId();

        OperationalRecommendation rec = repository.findRecommendationById(recommendationId)
                .orElseThrow(() -> new NotFoundException("Recommendation not found: " + recommendationId));

        if (hospitalId != null && !rec.getHospitalId().equals(hospitalId)) {
            throw new NotFoundException("Recommendation not found: " + recommendationId);
        }

        OperationalRecommendationDto dto = listUseCase.toDto(rec);

        dto.setAuditTrail(repository.findAuditByRecommendationId(recommendationId).stream()
                .map(a -> {
                    AuditEntryDto e = new AuditEntryDto();
                    e.setId(a.getId().toString());
                    e.setEventType(a.getEventType());
                    e.setEventLabel(a.getEventLabel());
                    e.setCreatedAt(a.getCreatedAt());
                    return e;
                }).collect(Collectors.toList()));

        dto.setTasks(repository.findTasksByRecommendationId(recommendationId).stream()
                .map(t -> {
                    TaskDto td = new TaskDto();
                    td.setId(t.getId().toString());
                    td.setOwnerLabel(t.getOwnerLabel());
                    td.setDepartmentLabel(t.getDepartmentLabel());
                    td.setPriority(t.getPriority());
                    td.setStatus(t.getStatus());
                    td.setDeadlineAt(t.getDeadlineAt());
                    td.setNotes(t.getNotes());
                    td.setCreatedAt(t.getCreatedAt());
                    return td;
                }).collect(Collectors.toList()));

        dto.setNotifications(repository.findNotificationsByRecommendationId(recommendationId).stream()
                .map(n -> {
                    NotificationDto nd = new NotificationDto();
                    nd.setId(n.getId().toString());
                    nd.setAudienceLabel(n.getAudienceLabel());
                    nd.setMessage(n.getMessage());
                    nd.setStatus(n.getStatus());
                    nd.setSentAt(n.getSentAt());
                    return nd;
                }).collect(Collectors.toList()));

        dto.setSupplyRequests(repository.findSupplyRequestsByRecommendationId(recommendationId).stream()
                .map(sr -> {
                    SupplyRequestItemDto sd = new SupplyRequestItemDto();
                    sd.setId(sr.getId().toString());
                    sd.setSupplyTypeLabel(sr.getSupplyTypeLabel());
                    sd.setQuantity(sr.getQuantity());
                    sd.setUnit(sr.getUnit());
                    sd.setDestination(sr.getDestination());
                    sd.setSuggestedSupplier(sr.getSuggestedSupplier());
                    sd.setStatus(sr.getStatus());
                    sd.setCreatedAt(sr.getCreatedAt());
                    return sd;
                }).collect(Collectors.toList()));

        return dto;
    }
}
