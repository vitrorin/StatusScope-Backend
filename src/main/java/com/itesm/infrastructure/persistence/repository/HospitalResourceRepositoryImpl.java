package com.itesm.infrastructure.persistence.repository;

import com.itesm.domain.models.HospitalDepartmentResource;
import com.itesm.domain.models.HospitalInventoryItem;
import com.itesm.domain.models.HospitalResourceSnapshot;
import com.itesm.domain.models.HospitalStaffingProfile;
import com.itesm.domain.repository.HospitalResourceRepository;
import com.itesm.infrastructure.persistence.entity.HospitalDepartmentResourceEntity;
import com.itesm.infrastructure.persistence.entity.HospitalEntity;
import com.itesm.infrastructure.persistence.entity.HospitalInventoryItemEntity;
import com.itesm.infrastructure.persistence.entity.HospitalResourceSnapshotEntity;
import com.itesm.infrastructure.persistence.entity.HospitalStaffingProfileEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class HospitalResourceRepositoryImpl
        implements HospitalResourceRepository,
                   PanacheRepositoryBase<HospitalResourceSnapshotEntity, UUID> {

    @Inject
    EntityManager em;

    @ConfigProperty(name = "quarkus.datasource.db-kind", defaultValue = "mysql")
    String dbKind;

    // -----------------------------------------------------------------------
    // Snapshots
    // -----------------------------------------------------------------------

    @Override
    public Optional<HospitalResourceSnapshot> findLatestSnapshotByHospitalId(UUID hospitalId) {
        return em.createQuery(
                "SELECT s FROM HospitalResourceSnapshotEntity s WHERE s.hospital.id = :hid ORDER BY s.capturedAt DESC",
                HospitalResourceSnapshotEntity.class)
                .setParameter("hid", hospitalId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .map(this::snapshotToDomain);
    }

    @Override
    public Optional<BigDecimal> findBedOccupancyPct(UUID hospitalId) {
        if (!"mysql".equalsIgnoreCase(dbKind)) {
            return Optional.empty();
        }
        Object result = em.createNativeQuery("SELECT fn_bed_occupancy_pct(?1)")
                .setParameter(1, hospitalId.toString())
                .getSingleResult();
        if (result == null) {
            return Optional.empty();
        }
        if (result instanceof BigDecimal value) {
            return Optional.of(value);
        }
        if (result instanceof Number value) {
            return Optional.of(BigDecimal.valueOf(value.doubleValue()));
        }
        return Optional.of(new BigDecimal(result.toString()));
    }

    @Override
    @Transactional
    public HospitalResourceSnapshot saveSnapshot(HospitalResourceSnapshot snapshot) {
        HospitalResourceSnapshotEntity e = new HospitalResourceSnapshotEntity();
        e.setId(snapshot.getId() != null ? snapshot.getId() : UUID.randomUUID());
        e.setHospital(em.getReference(HospitalEntity.class, snapshot.getHospitalId()));
        e.setCapturedAt(snapshot.getCapturedAt() != null ? snapshot.getCapturedAt() : LocalDateTime.now());
        e.setTotalBeds(snapshot.getTotalBeds());
        e.setAvailableBeds(snapshot.getAvailableBeds());
        e.setIcuTotalBeds(snapshot.getIcuTotalBeds());
        e.setIcuAvailableBeds(snapshot.getIcuAvailableBeds());
        e.setIsolationRoomsTotal(snapshot.getIsolationRoomsTotal());
        e.setIsolationRoomsAvailable(snapshot.getIsolationRoomsAvailable());
        e.setOxygenCapacityUnits(snapshot.getOxygenCapacityUnits());
        e.setOxygenAvailableUnits(snapshot.getOxygenAvailableUnits());
        e.setDoctorsOnShift(snapshot.getDoctorsOnShift());
        e.setNursesOnShift(snapshot.getNursesOnShift());
        e.setSpecialistsOnShift(snapshot.getSpecialistsOnShift());
        e.setSource(snapshot.getSource() != null ? snapshot.getSource() : "MANUAL");
        e.setCreatedAt(LocalDateTime.now());
        em.persist(e);
        snapshot.setId(e.getId());
        return snapshot;
    }

    // -----------------------------------------------------------------------
    // Departments
    // -----------------------------------------------------------------------

    @Override
    public List<HospitalDepartmentResource> findDepartmentsByHospitalId(UUID hospitalId) {
        return em.createQuery(
                "SELECT d FROM HospitalDepartmentResourceEntity d WHERE d.hospital.id = :hid ORDER BY d.departmentName ASC",
                HospitalDepartmentResourceEntity.class)
                .setParameter("hid", hospitalId)
                .getResultList()
                .stream().map(this::deptToDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<HospitalDepartmentResource> findDepartmentById(UUID id) {
        return Optional.ofNullable(em.find(HospitalDepartmentResourceEntity.class, id)).map(this::deptToDomain);
    }

    @Override
    @Transactional
    public HospitalDepartmentResource saveDepartment(HospitalDepartmentResource dept) {
        HospitalDepartmentResourceEntity e = dept.getId() != null
                ? em.find(HospitalDepartmentResourceEntity.class, dept.getId())
                : null;
        if (e == null) {
            e = new HospitalDepartmentResourceEntity();
            e.setId(dept.getId() != null ? dept.getId() : UUID.randomUUID());
            e.setHospital(em.getReference(HospitalEntity.class, dept.getHospitalId()));
        }
        e.setDepartmentCode(dept.getDepartmentCode());
        e.setDepartmentName(dept.getDepartmentName());
        e.setLevelLabel(dept.getLevelLabel());
        e.setTotalBeds(dept.getTotalBeds());
        e.setOccupiedBeds(dept.getOccupiedBeds());
        e.setStatus(dept.getStatus() != null ? dept.getStatus() : "NORMAL");
        e.setNotes(dept.getNotes());
        e.setUpdatedAt(LocalDateTime.now());
        em.persist(e);
        dept.setId(e.getId());
        return dept;
    }

    @Override
    @Transactional
    public void deleteDepartmentById(UUID id) {
        HospitalDepartmentResourceEntity entity = em.find(HospitalDepartmentResourceEntity.class, id);
        if (entity == null) {
            return;
        }
        em.remove(entity);
        em.flush();
    }

    // -----------------------------------------------------------------------
    // Staffing
    // -----------------------------------------------------------------------

    @Override
    public List<HospitalStaffingProfile> findStaffingByHospitalId(UUID hospitalId) {
        return em.createQuery(
                "SELECT s FROM HospitalStaffingProfileEntity s WHERE s.hospital.id = :hid ORDER BY s.roleName ASC",
                HospitalStaffingProfileEntity.class)
                .setParameter("hid", hospitalId)
                .getResultList()
                .stream().map(this::staffingToDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<HospitalStaffingProfile> findStaffingProfileById(UUID id) {
        return Optional.ofNullable(em.find(HospitalStaffingProfileEntity.class, id)).map(this::staffingToDomain);
    }

    @Override
    @Transactional
    public HospitalStaffingProfile saveStaffingProfile(HospitalStaffingProfile profile) {
        HospitalStaffingProfileEntity e = profile.getId() != null
                ? em.find(HospitalStaffingProfileEntity.class, profile.getId())
                : null;
        if (e == null) {
            e = new HospitalStaffingProfileEntity();
            e.setId(profile.getId() != null ? profile.getId() : UUID.randomUUID());
            e.setHospital(em.getReference(HospitalEntity.class, profile.getHospitalId()));
        }
        e.setRoleCode(profile.getRoleCode());
        e.setRoleName(profile.getRoleName());
        e.setHeadcount(profile.getHeadcount());
        e.setOnShiftCount(profile.getOnShiftCount());
        e.setOnCallCount(profile.getOnCallCount());
        e.setStandbyCount(profile.getStandbyCount());
        e.setUpdatedAt(LocalDateTime.now());
        em.persist(e);
        profile.setId(e.getId());
        return profile;
    }

    @Override
    @Transactional
    public void deleteStaffingProfileById(UUID id) {
        HospitalStaffingProfileEntity entity = em.find(HospitalStaffingProfileEntity.class, id);
        if (entity == null) {
            return;
        }
        em.remove(entity);
        em.flush();
    }

    // -----------------------------------------------------------------------
    // Inventory
    // -----------------------------------------------------------------------

    @Override
    public List<HospitalInventoryItem> findInventoryByHospitalId(UUID hospitalId) {
        return em.createQuery(
                "SELECT i FROM HospitalInventoryItemEntity i WHERE i.hospital.id = :hid ORDER BY i.category ASC, i.itemName ASC",
                HospitalInventoryItemEntity.class)
                .setParameter("hid", hospitalId)
                .getResultList()
                .stream().map(this::inventoryToDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<HospitalInventoryItem> findInventoryItemById(UUID id) {
        return Optional.ofNullable(em.find(HospitalInventoryItemEntity.class, id)).map(this::inventoryToDomain);
    }

    @Override
    @Transactional
    public HospitalInventoryItem saveInventoryItem(HospitalInventoryItem item) {
        HospitalInventoryItemEntity e = item.getId() != null
                ? em.find(HospitalInventoryItemEntity.class, item.getId())
                : null;
        if (e == null) {
            e = new HospitalInventoryItemEntity();
            e.setId(item.getId() != null ? item.getId() : UUID.randomUUID());
            e.setHospital(em.getReference(HospitalEntity.class, item.getHospitalId()));
        }
        e.setItemCode(item.getItemCode());
        e.setItemName(item.getItemName());
        e.setCategory(item.getCategory());
        e.setLocation(item.getLocation());
        e.setCurrentQuantity(item.getCurrentQuantity());
        e.setCapacityQuantity(item.getCapacityQuantity());
        e.setUnit(item.getUnit());
        e.setCriticalThreshold(item.getCriticalThreshold());
        e.setTargetQuantity(item.getTargetQuantity());
        e.setStatus(item.getStatus() != null ? item.getStatus() : inventoryStatus(item.getCurrentQuantity(), item.getCriticalThreshold()));
        e.setUpdatedAt(LocalDateTime.now());
        em.persist(e);
        em.flush();
        em.refresh(e);
        item.setId(e.getId());
        return inventoryToDomain(e);
    }

    private String inventoryStatus(int currentQuantity, int criticalThreshold) {
        if (currentQuantity <= criticalThreshold) return "CRITICAL";
        if (currentQuantity <= criticalThreshold * 2) return "LOW";
        return "ADEQUATE";
    }

    @Override
    @Transactional
    public void deleteInventoryItemById(UUID id) {
        HospitalInventoryItemEntity entity = em.find(HospitalInventoryItemEntity.class, id);
        if (entity == null) {
            return;
        }
        em.remove(entity);
        em.flush();
    }

    // -----------------------------------------------------------------------
    // Mapping helpers
    // -----------------------------------------------------------------------

    private HospitalResourceSnapshot snapshotToDomain(HospitalResourceSnapshotEntity e) {
        HospitalResourceSnapshot s = new HospitalResourceSnapshot();
        s.setId(e.getId());
        s.setHospitalId(e.getHospital().getId());
        s.setCapturedAt(e.getCapturedAt());
        s.setTotalBeds(e.getTotalBeds());
        s.setAvailableBeds(e.getAvailableBeds());
        s.setIcuTotalBeds(e.getIcuTotalBeds());
        s.setIcuAvailableBeds(e.getIcuAvailableBeds());
        s.setIsolationRoomsTotal(e.getIsolationRoomsTotal());
        s.setIsolationRoomsAvailable(e.getIsolationRoomsAvailable());
        s.setOxygenCapacityUnits(e.getOxygenCapacityUnits());
        s.setOxygenAvailableUnits(e.getOxygenAvailableUnits());
        s.setDoctorsOnShift(e.getDoctorsOnShift());
        s.setNursesOnShift(e.getNursesOnShift());
        s.setSpecialistsOnShift(e.getSpecialistsOnShift());
        s.setSource(e.getSource());
        s.setCreatedAt(e.getCreatedAt());
        return s;
    }

    private HospitalDepartmentResource deptToDomain(HospitalDepartmentResourceEntity e) {
        HospitalDepartmentResource d = new HospitalDepartmentResource();
        d.setId(e.getId());
        d.setHospitalId(e.getHospital().getId());
        d.setDepartmentCode(e.getDepartmentCode());
        d.setDepartmentName(e.getDepartmentName());
        d.setLevelLabel(e.getLevelLabel());
        d.setTotalBeds(e.getTotalBeds());
        d.setOccupiedBeds(e.getOccupiedBeds());
        d.setStatus(e.getStatus());
        d.setNotes(e.getNotes());
        d.setUpdatedAt(e.getUpdatedAt());
        return d;
    }

    private HospitalStaffingProfile staffingToDomain(HospitalStaffingProfileEntity e) {
        HospitalStaffingProfile p = new HospitalStaffingProfile();
        p.setId(e.getId());
        p.setHospitalId(e.getHospital().getId());
        p.setRoleCode(e.getRoleCode());
        p.setRoleName(e.getRoleName());
        p.setHeadcount(e.getHeadcount());
        p.setOnShiftCount(e.getOnShiftCount());
        p.setOnCallCount(e.getOnCallCount());
        p.setStandbyCount(e.getStandbyCount());
        p.setUpdatedAt(e.getUpdatedAt());
        return p;
    }

    private HospitalInventoryItem inventoryToDomain(HospitalInventoryItemEntity e) {
        HospitalInventoryItem i = new HospitalInventoryItem();
        i.setId(e.getId());
        i.setHospitalId(e.getHospital().getId());
        i.setItemCode(e.getItemCode());
        i.setItemName(e.getItemName());
        i.setCategory(e.getCategory());
        i.setLocation(e.getLocation());
        i.setCurrentQuantity(e.getCurrentQuantity());
        i.setCapacityQuantity(e.getCapacityQuantity());
        i.setUnit(e.getUnit());
        i.setCriticalThreshold(e.getCriticalThreshold());
        i.setTargetQuantity(e.getTargetQuantity());
        i.setStatus(e.getStatus());
        i.setUpdatedAt(e.getUpdatedAt());
        return i;
    }
}
