package com.itesm.application.usecase;

import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.HospitalDepartmentResource;
import com.itesm.domain.models.HospitalInventoryItem;
import com.itesm.domain.models.HospitalResourceSnapshot;
import com.itesm.domain.models.HospitalStaffingProfile;
import com.itesm.domain.repository.HospitalResourceRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class UpdateHospitalResourcesUseCase {

    @Inject AuthenticatedUserContext authenticatedUserContext;
    @Inject HospitalResourceRepository resourceRepository;

    @Transactional
    public HospitalResourceSnapshot updateSnapshot(HospitalResourceSnapshot input) {
        UUID hospitalId = resolveHospitalId();
        input.setHospitalId(hospitalId);
        return resourceRepository.saveSnapshot(input);
    }

    @Transactional
    public HospitalDepartmentResource updateDepartment(UUID departmentId, HospitalDepartmentResource input) {
        UUID hospitalId = resolveHospitalId();
        HospitalDepartmentResource existing = resourceRepository.findDepartmentById(departmentId)
                .orElseThrow(() -> new NotFoundException("Department not found: " + departmentId));
        if (!existing.getHospitalId().equals(hospitalId)) {
            throw new NotFoundException("Department not found: " + departmentId);
        }
        input.setId(departmentId);
        input.setHospitalId(hospitalId);
        return resourceRepository.saveDepartment(input);
    }

    @Transactional
    public HospitalStaffingProfile updateStaffingProfile(UUID profileId, HospitalStaffingProfile input) {
        UUID hospitalId = resolveHospitalId();
        HospitalStaffingProfile existing = resourceRepository.findStaffingProfileById(profileId)
                .orElseThrow(() -> new NotFoundException("Staffing profile not found: " + profileId));
        if (!existing.getHospitalId().equals(hospitalId)) {
            throw new NotFoundException("Staffing profile not found: " + profileId);
        }
        input.setId(profileId);
        input.setHospitalId(hospitalId);
        return resourceRepository.saveStaffingProfile(input);
    }

    @Transactional
    public HospitalInventoryItem updateInventoryItem(UUID itemId, HospitalInventoryItem input) {
        UUID hospitalId = resolveHospitalId();
        HospitalInventoryItem existing = resourceRepository.findInventoryItemById(itemId)
                .orElseThrow(() -> new NotFoundException("Inventory item not found: " + itemId));
        if (!existing.getHospitalId().equals(hospitalId)) {
            throw new NotFoundException("Inventory item not found: " + itemId);
        }
        input.setId(itemId);
        input.setHospitalId(hospitalId);
        return resourceRepository.saveInventoryItem(input);
    }

    private UUID resolveHospitalId() {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        UUID hospitalId = currentUser.getHospitalId();
        if (hospitalId == null) throw new NotFoundException("User has no assigned hospital");
        return hospitalId;
    }
}
