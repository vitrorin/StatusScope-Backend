package com.itesm.application.usecase;

import com.google.firebase.auth.FirebaseAuthException;
import com.itesm.application.dto.CreateUserByAdminDto;
import com.itesm.application.dto.UserSummaryDto;
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
import com.itesm.infrastructure.firebase.FirebaseUserService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import org.jboss.logging.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class CreateUserByAdminUseCase {

    private static final Logger LOG = Logger.getLogger(CreateUserByAdminUseCase.class);

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    HospitalRepository hospitalRepository;

    @Inject
    FirebaseUserService firebaseUserService;

    @Inject
    AuthenticatedUserContext authenticatedUserContext;

    @Inject
    AuthorizationService authorizationService;

    @RequiresPrivilege("users.manage")
    public UserSummaryDto execute(CreateUserByAdminDto dto) {
        CurrentUser caller = authenticatedUserContext.getCurrentUser();

        // Enforce hospital scoping
        UUID targetHospitalId;
        if (caller.isSystemAdmin()) {
            if ("SYSTEM_ADMIN".equals(dto.getRoleCode())) {
                targetHospitalId = null;
            } else {
                if (dto.getHospitalId() == null) {
                    throw new IllegalArgumentException("hospitalId is required when creating non-SYSTEM_ADMIN users");
                }
                targetHospitalId = dto.getHospitalId();
            }
        } else {
            // HOSPITAL_ADMIN: always scoped to own hospital, cannot create SYSTEM_ADMIN
            if ("SYSTEM_ADMIN".equals(dto.getRoleCode())) {
                throw new ForbiddenException("HOSPITAL_ADMIN cannot create SYSTEM_ADMIN users");
            }
            targetHospitalId = caller.getHospitalId();
        }

        // Validate hospital exists (unless SYSTEM_ADMIN with no hospital)
        Hospital hospital = null;
        if (targetHospitalId != null) {
            hospital = hospitalRepository.findHospitalById(targetHospitalId)
                    .orElseThrow(() -> new NotFoundException("Hospital not found"));
        }

        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new ConflictException("Email already registered");
        }

        Role role = roleRepository.findByCode(dto.getRoleCode())
                .orElseThrow(() -> new NotFoundException("Role not found: " + dto.getRoleCode()));

        String uid;
        try {
            uid = firebaseUserService.createUser(dto.getEmail(), dto.getPassword(), dto.getFullName());
        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Failed to create Firebase user: " + e.getMessage(), e);
        }

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setExternalAuthId(uid);
        user.setHospitalId(targetHospitalId);
        user.setStatus(UserStatus.ACTIVE);
        user.setActive(true);
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);

        try {
            userRepository.create(user);
        } catch (Exception e) {
            try {
                firebaseUserService.deleteUser(uid);
            } catch (FirebaseAuthException compensateEx) {
                LOG.errorf("COMPENSATION FAILED: could not delete Firebase user %s: %s", uid, compensateEx.getMessage());
            }
            throw e;
        }

        UserSummaryDto summary = new UserSummaryDto();
        summary.setId(user.getId());
        summary.setEmail(user.getEmail());
        summary.setFullName(user.getFullName());
        summary.setHospitalId(targetHospitalId);
        summary.setHospitalName(hospital != null ? hospital.getName() : null);
        summary.setRoles(user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet()));
        summary.setPrivileges(user.getRoles().stream()
                .flatMap(r -> r.getPrivileges().stream())
                .map(p -> p.getCode())
                .collect(Collectors.toSet()));
        return summary;
    }
}
