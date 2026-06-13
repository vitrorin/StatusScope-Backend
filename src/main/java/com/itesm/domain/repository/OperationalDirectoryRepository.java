package com.itesm.domain.repository;

import com.itesm.domain.models.HospitalInventoryMovement;
import com.itesm.domain.models.HospitalOperationalContact;
import com.itesm.domain.models.HospitalOperationalGroup;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OperationalDirectoryRepository {
    List<HospitalOperationalContact> findContactsByHospitalId(UUID hospitalId, Boolean assignable, Boolean notifiable, String departmentCode);
    List<HospitalOperationalContact> findActiveEmailContactsByDepartment(UUID hospitalId, String departmentCode, boolean notifiableOnly);
    Optional<HospitalOperationalContact> findContactById(UUID contactId);
    HospitalOperationalContact createContact(HospitalOperationalContact contact);
    HospitalOperationalContact updateContact(HospitalOperationalContact contact);

    List<HospitalOperationalGroup> findGroupsByHospitalId(UUID hospitalId, Boolean assignable, Boolean notifiable, String departmentCode);
    Optional<HospitalOperationalGroup> findGroupById(UUID groupId);

    List<HospitalInventoryMovement> findInventoryMovementsByItemId(UUID hospitalId, UUID inventoryItemId);
    void appendInventoryMovement(HospitalInventoryMovement movement);
}
