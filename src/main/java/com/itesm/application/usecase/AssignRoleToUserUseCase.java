package com.itesm.application.usecase;

import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.AuthorizationService;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.Role;
import com.itesm.domain.models.User;
import com.itesm.domain.repository.RoleRepository;
import com.itesm.domain.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AssignRoleToUserUseCase {

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    AuthenticatedUserContext authenticatedUserContext;

    @Inject
    AuthorizationService authorizationService;

    public User execute(String roleCode, java.util.UUID userId) {
        CurrentUser caller = authenticatedUserContext.getCurrentUser();
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!caller.isSystemAdmin()) {
            authorizationService.assertSameHospital(user.getHospitalId());
        }

        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new NotFoundException("Role not found"));

        user.getRoles().add(role);
        return userRepository.update(user);
    }
}
