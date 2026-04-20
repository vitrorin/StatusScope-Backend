package com.itesm.application.usecase;

import com.itesm.application.dto.RegisterUserDto;
import com.itesm.application.usecase.exception.ConflictException;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.Role;
import com.itesm.domain.models.User;
import com.itesm.domain.repository.RoleRepository;
import com.itesm.domain.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class RegisterUserUseCase {

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    public User execute(RegisterUserDto registerUserDto) {
        if (userRepository.findByEmail(registerUserDto.getEmail()).isPresent()) {
            throw new ConflictException("Email already registered");
        }
        if (userRepository.findByExternalAuthId(registerUserDto.getExternalAuthId()).isPresent()) {
            throw new ConflictException("External auth id already registered");
        }

        Role defaultRole = roleRepository.findByCode("DOCTOR")
                .orElseThrow(() -> new NotFoundException("Default role DOCTOR not found"));

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setActive(true);
        user.setFullName(registerUserDto.getFullName());
        user.setEmail(registerUserDto.getEmail());
        user.setExternalAuthId(registerUserDto.getExternalAuthId());

        Set<Role> roles = new HashSet<>();
        roles.add(defaultRole);
        user.setRoles(roles);

        return userRepository.create(user);
    }
}
