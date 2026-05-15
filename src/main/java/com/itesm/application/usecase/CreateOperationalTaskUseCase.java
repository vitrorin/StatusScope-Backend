package com.itesm.application.usecase;

import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.OperationalRecommendation;
import com.itesm.domain.models.OperationalRecommendationAudit;
import com.itesm.domain.models.OperationalTask;
import com.itesm.domain.models.HospitalOperationalContact;
import com.itesm.domain.models.HospitalOperationalGroup;
import com.itesm.domain.repository.OperationalDirectoryRepository;
import com.itesm.domain.repository.OperationalRecommendationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class CreateOperationalTaskUseCase {

    @Inject AuthenticatedUserContext authenticatedUserContext;
    @Inject OperationalRecommendationRepository repository;
    @Inject OperationalDirectoryRepository operationalDirectoryRepository;

    @Transactional
    public OperationalTask execute(UUID recommendationId, OperationalTask input) {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        UUID hospitalId = currentUser.getHospitalId();

        OperationalRecommendation rec = repository.findRecommendationById(recommendationId)
                .orElseThrow(() -> new NotFoundException("Recommendation not found: " + recommendationId));

        if (hospitalId != null && !rec.getHospitalId().equals(hospitalId)) {
            throw new NotFoundException("Recommendation not found: " + recommendationId);
        }

        input.setRecommendationId(recommendationId);
        input.setHospitalId(rec.getHospitalId());
        input.setCreatedByUserId(currentUser.getUserId());
        input.setStatus(input.getStatus() != null ? input.getStatus() : "PENDING");
        input.setPriority(input.getPriority() != null ? input.getPriority() : "MEDIUM");
        input.setSourceActionCode(input.getSourceActionCode() != null ? input.getSourceActionCode() : "ASSIGN_TASK");
        input.setRecommendedByRecommendationId(recommendationId);

        if (input.getOwnerContactId() != null) {
            HospitalOperationalContact contact = operationalDirectoryRepository.findContactById(input.getOwnerContactId()).orElse(null);
            if (contact != null) {
                input.setOwnerLabel(input.getOwnerLabel() != null ? input.getOwnerLabel() : contact.getDisplayName());
                input.setDepartmentLabel(input.getDepartmentLabel() != null ? input.getDepartmentLabel() : contact.getDepartmentCode());
                input.setOwnerUserId(input.getOwnerUserId() != null ? input.getOwnerUserId() : contact.getUserId());
            }
        }
        if (input.getOwnerGroupId() != null) {
            HospitalOperationalGroup group = operationalDirectoryRepository.findGroupById(input.getOwnerGroupId()).orElse(null);
            if (group != null && input.getOwnerLabel() == null) {
                input.setOwnerLabel(group.getGroupName());
            }
        }

        OperationalTask created = repository.createTask(input);

        // Update recommendation status to ASSIGNED if it was NEW or ACCEPTED
        if ("NEW".equals(rec.getStatus()) || "ACCEPTED".equals(rec.getStatus())) {
            repository.updateStatus(recommendationId, "ASSIGNED");
        }
        if (input.getOwnerUserId() != null) {
            rec.setAssignedOwnerUserId(input.getOwnerUserId());
            rec.setStatus("ASSIGNED");
            repository.save(rec);
        }

        OperationalRecommendationAudit audit = new OperationalRecommendationAudit();
        audit.setRecommendationId(recommendationId);
        audit.setActorUserId(currentUser.getUserId());
        audit.setEventType("TASK_CREATED");
        audit.setEventLabel("Task assigned to: " + (input.getOwnerLabel() != null ? input.getOwnerLabel() : "unassigned"));
        audit.setCreatedAt(LocalDateTime.now());
        repository.appendAudit(audit);

        return created;
    }
}
