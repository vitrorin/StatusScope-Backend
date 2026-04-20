package com.itesm.application.usecase;

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

    public User execute(String roleCode, java.util.UUID userId) {
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new NotFoundException("Role not found"));

        user.getRoles().add(role);
        return userRepository.update(user);
    }
}
