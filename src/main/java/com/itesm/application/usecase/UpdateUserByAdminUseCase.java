package com.itesm.application.usecase;

import com.itesm.application.dto.AdminUserDto;
import com.itesm.application.dto.UpdateUserByAdminDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.AuthorizationService;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.security.RequiresPrivilege;
import com.itesm.application.usecase.exception.ConflictException;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.Hospital;
import com.itesm.domain.models.Role;
import com.itesm.domain.models.User;
import com.itesm.domain.models.UserStatus;
import com.itesm.domain.repository.HospitalRepository;
import com.itesm.domain.repository.RoleRepository;
import com.itesm.domain.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;

import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class UpdateUserByAdminUseCase {

    @Inject UserRepository userRepository;
    @Inject RoleRepository roleRepository;
    @Inject HospitalRepository hospitalRepository;
    @Inject AuthenticatedUserContext authenticatedUserContext;
    @Inject AuthorizationService authorizationService;

    @RequiresPrivilege("users.manage")
    public AdminUserDto execute(UUID userId, UpdateUserByAdminDto dto) {
        CurrentUser caller = authenticatedUserContext.getCurrentUser();
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        UUID targetHospitalId = resolveTargetHospital(caller, dto);
        if (!caller.isSystemAdmin()) {
            authorizationService.assertSameHospital(user.getHospitalId());
        }

        userRepository.findByEmail(dto.getEmail())
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new ConflictException("Email already registered");
                });

        Role role = roleRepository.findByCode(dto.getRoleCode())
                .orElseThrow(() -> new NotFoundException("Role not found: " + dto.getRoleCode()));

        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setHospitalId(targetHospitalId);
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            user.setStatus(UserStatus.valueOf(dto.getStatus()));
            user.setActive(user.getStatus() == UserStatus.ACTIVE);
        }
        user.setRoles(new HashSet<>(java.util.List.of(role)));

        User updated = userRepository.update(user);
        Map<UUID, String> hospitalNamesById = hospitalRepository.listAllDomain().stream()
                .collect(Collectors.toMap(Hospital::getId, Hospital::getName));
        return toAdminUserDto(updated, hospitalNamesById);
    }

    private UUID resolveTargetHospital(CurrentUser caller, UpdateUserByAdminDto dto) {
        if (caller.isSystemAdmin()) {
            if ("SYSTEM_ADMIN".equals(dto.getRoleCode())) {
                return null;
            }
            if (dto.getHospitalId() == null) {
                throw new IllegalArgumentException("hospitalId is required when assigning hospital-scoped roles");
            }
            hospitalRepository.findHospitalById(dto.getHospitalId())
                    .orElseThrow(() -> new NotFoundException("Hospital not found"));
            return dto.getHospitalId();
        }
        if ("SYSTEM_ADMIN".equals(dto.getRoleCode())) {
            throw new ForbiddenException("HOSPITAL_ADMIN cannot assign SYSTEM_ADMIN role");
        }
        return caller.getHospitalId();
    }

    private AdminUserDto toAdminUserDto(User user, Map<UUID, String> hospitalNamesById) {
        AdminUserDto dto = new AdminUserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setHospitalId(user.getHospitalId());
        dto.setHospitalName(user.getHospitalId() == null ? null : hospitalNamesById.get(user.getHospitalId()));
        dto.setStatus(user.getStatus() == null ? null : user.getStatus().name());
        dto.setRoleCodes(user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet()));
        return dto;
    }
}
