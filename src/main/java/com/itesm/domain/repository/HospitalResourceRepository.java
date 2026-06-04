package com.itesm.domain.repository;

import com.itesm.domain.models.HospitalDepartmentResource;
import com.itesm.domain.models.HospitalInventoryItem;
import com.itesm.domain.models.HospitalResourceSnapshot;
import com.itesm.domain.models.HospitalStaffingProfile;

import java.util.List;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface HospitalResourceRepository {
    Optional<HospitalResourceSnapshot> findLatestSnapshotByHospitalId(UUID hospitalId);
    Optional<BigDecimal> findBedOccupancyPct(UUID hospitalId);
    HospitalResourceSnapshot saveSnapshot(HospitalResourceSnapshot snapshot);

    List<HospitalDepartmentResource> findDepartmentsByHospitalId(UUID hospitalId);
    Optional<HospitalDepartmentResource> findDepartmentById(UUID id);
    HospitalDepartmentResource saveDepartment(HospitalDepartmentResource department);
    void deleteDepartmentById(UUID id);

    List<HospitalStaffingProfile> findStaffingByHospitalId(UUID hospitalId);
    Optional<HospitalStaffingProfile> findStaffingProfileById(UUID id);
    HospitalStaffingProfile saveStaffingProfile(HospitalStaffingProfile profile);
    void deleteStaffingProfileById(UUID id);

    List<HospitalInventoryItem> findInventoryByHospitalId(UUID hospitalId);
    Optional<HospitalInventoryItem> findInventoryItemById(UUID id);
    HospitalInventoryItem saveInventoryItem(HospitalInventoryItem item);
    void deleteInventoryItemById(UUID id);
}
