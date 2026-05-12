package com.itesm.application.usecase;

import com.itesm.application.dto.HospitalInventoryMovementDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.HospitalInventoryItem;
import com.itesm.domain.repository.HospitalResourceRepository;
import com.itesm.domain.repository.OperationalDirectoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class GetInventoryMovementHistoryUseCase {

    @Inject AuthenticatedUserContext authenticatedUserContext;
    @Inject HospitalResourceRepository hospitalResourceRepository;
    @Inject OperationalDirectoryRepository operationalDirectoryRepository;

    public List<HospitalInventoryMovementDto> execute(UUID itemId) {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        UUID hospitalId = currentUser.getHospitalId();
        if (hospitalId == null) {
            throw new NotFoundException("User has no assigned hospital");
        }

        HospitalInventoryItem item = hospitalResourceRepository.findInventoryItemById(itemId)
                .orElseThrow(() -> new NotFoundException("Inventory item not found: " + itemId));
        if (!hospitalId.equals(item.getHospitalId())) {
            throw new NotFoundException("Inventory item not found: " + itemId);
        }

        return operationalDirectoryRepository.findInventoryMovementsByItemId(hospitalId, itemId)
                .stream()
                .map(movement -> {
                    HospitalInventoryMovementDto dto = new HospitalInventoryMovementDto();
                    dto.setId(movement.getId().toString());
                    dto.setInventoryItemId(movement.getInventoryItemId().toString());
                    dto.setMovementType(movement.getMovementType());
                    dto.setQuantityDelta(movement.getQuantityDelta());
                    dto.setUnit(movement.getUnit());
                    dto.setNotes(movement.getNotes());
                    if (movement.getRelatedSupplyRequestId() != null) {
                        dto.setRelatedSupplyRequestId(movement.getRelatedSupplyRequestId().toString());
                    }
                    dto.setCreatedAt(movement.getCreatedAt());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
