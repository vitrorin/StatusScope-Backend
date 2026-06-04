package com.itesm.application.usecase;

import com.itesm.application.dto.HospitalResourcesDto;
import com.itesm.application.dto.HospitalResourcesDto.DepartmentDto;
import com.itesm.application.dto.HospitalResourcesDto.InventoryItemDto;
import com.itesm.application.dto.HospitalResourcesDto.ResourceSummaryDto;
import com.itesm.application.dto.HospitalResourcesDto.StaffingProfileDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.HospitalResourceSnapshot;
import com.itesm.domain.repository.HospitalResourceRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class GetHospitalResourcesUseCase {

    @Inject AuthenticatedUserContext authenticatedUserContext;
    @Inject HospitalResourceRepository resourceRepository;

    public HospitalResourcesDto execute() {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        UUID hospitalId = currentUser.getHospitalId();
        if (hospitalId == null) throw new NotFoundException("User has no assigned hospital");

        HospitalResourcesDto dto = new HospitalResourcesDto();
        dto.setHospitalId(hospitalId.toString());
        dto.setGeneratedAt(LocalDateTime.now());

        HospitalResourceSnapshot snapshot = resourceRepository
                .findLatestSnapshotByHospitalId(hospitalId).orElse(null);
        dto.setSummary(toSummaryDto(snapshot, hospitalId));

        dto.setDepartments(resourceRepository.findDepartmentsByHospitalId(hospitalId)
                .stream().map(this::toDeptDto).collect(Collectors.toList()));

        dto.setStaffing(resourceRepository.findStaffingByHospitalId(hospitalId)
                .stream().map(this::toStaffingDto).collect(Collectors.toList()));

        dto.setInventory(resourceRepository.findInventoryByHospitalId(hospitalId)
                .stream().map(this::toInventoryDto).collect(Collectors.toList()));

        return dto;
    }

    private ResourceSummaryDto toSummaryDto(HospitalResourceSnapshot s, UUID hospitalId) {
        ResourceSummaryDto dto = new ResourceSummaryDto();
        if (s == null) return dto;
        dto.setTotalBeds(s.getTotalBeds());
        dto.setAvailableBeds(s.getAvailableBeds());
        dto.setIcuTotalBeds(s.getIcuTotalBeds());
        dto.setIcuAvailableBeds(s.getIcuAvailableBeds());
        dto.setIsolationRoomsTotal(s.getIsolationRoomsTotal());
        dto.setIsolationRoomsAvailable(s.getIsolationRoomsAvailable());
        dto.setOxygenCapacityUnits(s.getOxygenCapacityUnits());
        dto.setOxygenAvailableUnits(s.getOxygenAvailableUnits());
        dto.setDoctorsOnShift(s.getDoctorsOnShift());
        dto.setNursesOnShift(s.getNursesOnShift());
        dto.setSpecialistsOnShift(s.getSpecialistsOnShift());
        dto.setBedOccupancyPct(resourceRepository.findBedOccupancyPct(hospitalId)
                .orElseGet(() -> occupancyPct(s.getTotalBeds(), s.getAvailableBeds())));
        dto.setIcuOccupancyPct(occupancyPct(s.getIcuTotalBeds(), s.getIcuAvailableBeds()));
        dto.setSource(s.getSource());
        dto.setCapturedAt(s.getCapturedAt());
        return dto;
    }

    private BigDecimal occupancyPct(int total, int available) {
        if (total <= 0) return null;
        return BigDecimal.valueOf(total - available)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private DepartmentDto toDeptDto(com.itesm.domain.models.HospitalDepartmentResource d) {
        DepartmentDto dto = new DepartmentDto();
        dto.setId(d.getId().toString());
        dto.setDepartmentCode(d.getDepartmentCode());
        dto.setDepartmentName(d.getDepartmentName());
        dto.setLevelLabel(d.getLevelLabel());
        dto.setTotalBeds(d.getTotalBeds());
        dto.setOccupiedBeds(d.getOccupiedBeds());
        dto.setAvailableBeds(d.getTotalBeds() - d.getOccupiedBeds());
        dto.setStatus(d.getStatus());
        dto.setNotes(d.getNotes());
        return dto;
    }

    private StaffingProfileDto toStaffingDto(com.itesm.domain.models.HospitalStaffingProfile p) {
        StaffingProfileDto dto = new StaffingProfileDto();
        dto.setId(p.getId().toString());
        dto.setRoleCode(p.getRoleCode());
        dto.setRoleName(p.getRoleName());
        dto.setHeadcount(p.getHeadcount());
        dto.setOnShiftCount(p.getOnShiftCount());
        dto.setOnCallCount(p.getOnCallCount());
        dto.setStandbyCount(p.getStandbyCount());
        return dto;
    }

    private InventoryItemDto toInventoryDto(com.itesm.domain.models.HospitalInventoryItem i) {
        InventoryItemDto dto = new InventoryItemDto();
        dto.setId(i.getId().toString());
        dto.setItemCode(i.getItemCode());
        dto.setItemName(i.getItemName());
        dto.setCategory(i.getCategory());
        dto.setLocation(i.getLocation());
        dto.setCurrentQuantity(i.getCurrentQuantity());
        dto.setCapacityQuantity(i.getCapacityQuantity());
        dto.setUnit(i.getUnit());
        dto.setCriticalThreshold(i.getCriticalThreshold());
        dto.setTargetQuantity(i.getTargetQuantity());
        dto.setStatus(i.getStatus());
        return dto;
    }
}
