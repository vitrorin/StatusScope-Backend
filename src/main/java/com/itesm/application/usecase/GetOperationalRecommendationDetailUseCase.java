package com.itesm.application.usecase;

import com.itesm.application.dto.OperationalRecommendationDto;
import com.itesm.application.dto.OperationalRecommendationDto.AuditEntryDto;
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

        dto.setSupplyRequests(repository.findSupplyRequestsByRecommendationId(recommendationId).stream()
                .map(sr -> {
                    SupplyRequestItemDto sd = new SupplyRequestItemDto();
                    sd.setId(sr.getId().toString());
                    if (sr.getInventoryItemId() != null) sd.setInventoryItemId(sr.getInventoryItemId().toString());
                    sd.setSupplyTypeLabel(sr.getSupplyTypeLabel());
                    sd.setQuantity(sr.getQuantity());
                    sd.setUnit(sr.getUnit());
                    sd.setDestination(sr.getDestination());
                    sd.setSuggestedSupplier(sr.getSuggestedSupplier());
                    sd.setStatus(sr.getStatus());
                    sd.setSourceActionCode(sr.getSourceActionCode());
                    sd.setPriority(sr.getPriority());
                    sd.setRequestedNeededBy(sr.getRequestedNeededBy());
                    sd.setCreatedAt(sr.getCreatedAt());
                    return sd;
                }).collect(Collectors.toList()));

        return dto;
    }
}
