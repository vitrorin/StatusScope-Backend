package com.itesm.application.usecase;

import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.OperationalRecommendation;
import com.itesm.domain.models.OperationalRecommendationAudit;
import com.itesm.domain.models.SupplyRequest;
import com.itesm.domain.repository.OperationalRecommendationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class CreateSupplyRequestUseCase {

    @Inject AuthenticatedUserContext authenticatedUserContext;
    @Inject OperationalRecommendationRepository repository;

    @Transactional
    public SupplyRequest execute(UUID recommendationId, SupplyRequest input) {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        UUID hospitalId = currentUser.getHospitalId();

        OperationalRecommendation rec = repository.findRecommendationById(recommendationId)
                .orElseThrow(() -> new NotFoundException("Recommendation not found: " + recommendationId));

        if (hospitalId != null && !rec.getHospitalId().equals(hospitalId)) {
            throw new NotFoundException("Recommendation not found: " + recommendationId);
        }

        input.setRecommendationId(recommendationId);
        input.setHospitalId(rec.getHospitalId());
        input.setRequestedByUserId(currentUser.getUserId());
        input.setStatus("REQUESTED");

        SupplyRequest created = repository.createSupplyRequest(input);

        OperationalRecommendationAudit audit = new OperationalRecommendationAudit();
        audit.setRecommendationId(recommendationId);
        audit.setActorUserId(currentUser.getUserId());
        audit.setEventType("SUPPLY_REQUESTED");
        audit.setEventLabel("Supply order: " + input.getQuantity() + " " +
                (input.getUnit() != null ? input.getUnit() + " " : "") +
                (input.getSupplyTypeLabel() != null ? input.getSupplyTypeLabel() : "units"));
        audit.setCreatedAt(LocalDateTime.now());
        repository.appendAudit(audit);

        return created;
    }
}
