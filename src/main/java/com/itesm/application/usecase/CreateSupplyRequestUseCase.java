package com.itesm.application.usecase;

import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.OperationalRecommendation;
import com.itesm.domain.models.OperationalRecommendationAudit;
import com.itesm.domain.models.SupplyRequest;
import com.itesm.domain.models.HospitalInventoryMovement;
import com.itesm.domain.models.HospitalInventoryItem;
import com.itesm.domain.repository.OperationalDirectoryRepository;
import com.itesm.domain.repository.OperationalRecommendationRepository;
import com.itesm.domain.repository.HospitalResourceRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class CreateSupplyRequestUseCase {

    @Inject AuthenticatedUserContext authenticatedUserContext;
    @Inject OperationalRecommendationRepository repository;
    @Inject OperationalDirectoryRepository operationalDirectoryRepository;
    @Inject HospitalResourceRepository hospitalResourceRepository;

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
        input.setSourceActionCode(input.getSourceActionCode() != null ? input.getSourceActionCode() : "ORDER_SUPPLIES");
        input.setPriority(input.getPriority() != null ? input.getPriority() : "HIGH");
        input.setLinkedRecommendationInventoryItemId(
                input.getLinkedRecommendationInventoryItemId() != null
                        ? input.getLinkedRecommendationInventoryItemId()
                        : rec.getPrimaryInventoryItemId());
        input.setInventoryItemId(input.getInventoryItemId() != null ? input.getInventoryItemId() : rec.getPrimaryInventoryItemId());

        SupplyRequest created = repository.createSupplyRequestWithMovement(
                input,
                "Supply request created from recommendation " + recommendationId);

        OperationalRecommendationAudit audit = new OperationalRecommendationAudit();
        audit.setRecommendationId(recommendationId);
        audit.setActorUserId(currentUser.getUserId());
        audit.setEventType("SUPPLY_REQUESTED");
        audit.setEventLabel("Supply order: " + input.getQuantity() + " " +
                (input.getUnit() != null ? input.getUnit() + " " : "") +
                (input.getSupplyTypeLabel() != null ? input.getSupplyTypeLabel() : "units"));
        repository.appendAudit(audit);

        return created;
    }

    @Transactional
    public SupplyRequest executeForInventory(UUID inventoryItemId, SupplyRequest input) {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        UUID hospitalId = currentUser.getHospitalId();
        if (hospitalId == null) {
            throw new NotFoundException("User has no assigned hospital");
        }

        HospitalInventoryItem item = hospitalResourceRepository.findInventoryItemById(inventoryItemId)
                .orElseThrow(() -> new NotFoundException("Inventory item not found: " + inventoryItemId));
        if (!hospitalId.equals(item.getHospitalId())) {
            throw new NotFoundException("Inventory item not found: " + inventoryItemId);
        }

        input.setRecommendationId(null);
        input.setHospitalId(hospitalId);
        input.setInventoryItemId(inventoryItemId);
        input.setRequestedByUserId(currentUser.getUserId());
        input.setStatus("REQUESTED");
        input.setSourceActionCode(input.getSourceActionCode() != null ? input.getSourceActionCode() : "RESOURCE_SUPPLY_REQUEST");
        input.setPriority(input.getPriority() != null ? input.getPriority() : "MEDIUM");
        input.setSupplyTypeLabel(input.getSupplyTypeLabel() != null ? input.getSupplyTypeLabel() : item.getItemName());
        input.setUnit(input.getUnit() != null ? input.getUnit() : item.getUnit());
        input.setDestination(input.getDestination() != null ? input.getDestination() : item.getLocation());

        SupplyRequest created = repository.createSupplyRequest(input);

        HospitalInventoryMovement movement = new HospitalInventoryMovement();
        movement.setHospitalId(hospitalId);
        movement.setInventoryItemId(inventoryItemId);
        movement.setMovementType("REPLENISHMENT");
        movement.setQuantityDelta(created.getQuantity());
        movement.setUnit(created.getUnit());
        movement.setNotes("Supply request created from resources inventory screen");
        movement.setRelatedSupplyRequestId(created.getId());
        movement.setCreatedAt(LocalDateTime.now());
        operationalDirectoryRepository.appendInventoryMovement(movement);

        return created;
    }
}
