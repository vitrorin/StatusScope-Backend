package com.itesm.application.usecase;

import com.google.firebase.auth.FirebaseAuthException;
import com.itesm.application.dto.RegisterUserDto;
import com.itesm.application.dto.UserSummaryDto;
import com.itesm.application.usecase.exception.ConflictException;
import com.itesm.application.usecase.exception.InvalidInviteException;
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
import org.jboss.logging.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class RegisterUserUseCase {

    private static final Logger LOG = Logger.getLogger(RegisterUserUseCase.class);

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    HospitalRepository hospitalRepository;

    @Inject
    FirebaseUserService firebaseUserService;

    public UserSummaryDto execute(RegisterUserDto dto) {
        // 1. Resolve hospital from invite code (fail-fast before touching Firebase)
        Hospital hospital = hospitalRepository.findByInviteCode(dto.getInviteCode())
                .filter(Hospital::isActive)
                .orElseThrow(() -> new InvalidInviteException("Invalid or inactive invite code"));

        // 2. Check for email conflict
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new ConflictException("Email already registered");
        }

        // 3. Find default DOCTOR role
        Role doctorRole = roleRepository.findByCode("DOCTOR")
                .orElseThrow(() -> new RuntimeException("Default role DOCTOR not found"));

        // 4. Create Firebase user
        String uid;
        try {
            uid = firebaseUserService.createUser(dto.getEmail(), dto.getPassword(), dto.getFullName());
        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Failed to create Firebase user: " + e.getMessage(), e);
        }

        // 5. Insert DB row with compensation on failure
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setExternalAuthId(uid);
        user.setHospitalId(hospital.getId());
        user.setStatus(UserStatus.ACTIVE);
        user.setActive(true);
        Set<Role> roles = new HashSet<>();
        roles.add(doctorRole);
        user.setRoles(roles);

        try {
            userRepository.create(user);
        } catch (Exception e) {
            try {
                firebaseUserService.deleteUser(uid);
            } catch (FirebaseAuthException compensateEx) {
                LOG.errorf("COMPENSATION FAILED: could not delete Firebase user %s after DB failure: %s", uid, compensateEx.getMessage());
            }
            throw e;
        }

        return toSummary(user, hospital.getName());
    }

    private UserSummaryDto toSummary(User user, String hospitalName) {
        UserSummaryDto dto = new UserSummaryDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setHospitalId(user.getHospitalId());
        dto.setHospitalName(hospitalName);
        dto.setRoles(user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet()));
        dto.setPrivileges(user.getRoles().stream()
                .flatMap(r -> r.getPrivileges().stream())
                .map(p -> p.getCode())
                .collect(Collectors.toSet()));
        return dto;
    }
}

