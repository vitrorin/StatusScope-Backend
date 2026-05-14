package com.itesm.application.usecase;

import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.OperationalRecommendation;
import com.itesm.domain.models.OperationalRecommendationAudit;
import com.itesm.domain.repository.OperationalRecommendationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class UpdateOperationalRecommendationStatusUseCase {

    private static final Set<String> VALID_STATUSES = Set.of(
            "NEW", "ACCEPTED", "ASSIGNED", "COMPLETED", "REJECTED", "IN_PROGRESS");

    @Inject AuthenticatedUserContext authenticatedUserContext;
    @Inject OperationalRecommendationRepository repository;

    @Transactional
    public void execute(UUID recommendationId, String newStatus) {
        if (!VALID_STATUSES.contains(newStatus.toUpperCase())) {
            throw new IllegalArgumentException("Invalid status: " + newStatus);
        }

        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        UUID hospitalId = currentUser.getHospitalId();

        OperationalRecommendation rec = repository.findRecommendationById(recommendationId)
                .orElseThrow(() -> new NotFoundException("Recommendation not found: " + recommendationId));

        if (hospitalId != null && !rec.getHospitalId().equals(hospitalId)) {
            throw new NotFoundException("Recommendation not found: " + recommendationId);
        }

        repository.updateStatus(recommendationId, newStatus.toUpperCase());

        OperationalRecommendationAudit audit = new OperationalRecommendationAudit();
        audit.setRecommendationId(recommendationId);
        audit.setActorUserId(currentUser.getUserId());
        audit.setEventType("STATUS_CHANGED");
        audit.setEventLabel("Status changed to " + newStatus.toUpperCase());
        audit.setCreatedAt(LocalDateTime.now());
        repository.appendAudit(audit);
    }
}
