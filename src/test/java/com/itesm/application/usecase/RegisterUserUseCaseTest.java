package com.itesm.application.usecase;

import com.itesm.application.dto.RegisterUserDto;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.Role;
import com.itesm.domain.models.User;
import com.itesm.domain.repository.RoleRepository;
import com.itesm.domain.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

class RegisterUserUseCaseTest {

    private RegisterUserUseCase useCase;
    private UserRepository userRepository;
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        useCase = new RegisterUserUseCase();
        userRepository = Mockito.mock(UserRepository.class);
        roleRepository = Mockito.mock(RoleRepository.class);
        useCase.userRepository = userRepository;
        useCase.roleRepository = roleRepository;
    }

    @Test
    void shouldAssignDefaultRoleOnRegister() {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setFullName("Dr. New");
        dto.setEmail("new@statusscope.local");
        dto.setExternalAuthId("ext-new");

        Role doctor = new Role();
        doctor.setId(UUID.randomUUID());
        doctor.setCode("DOCTOR");

        Mockito.when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
        Mockito.when(userRepository.findByExternalAuthId(dto.getExternalAuthId())).thenReturn(Optional.empty());
        Mockito.when(roleRepository.findByCode("DOCTOR")).thenReturn(Optional.of(doctor));
        Mockito.when(userRepository.create(Mockito.any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = useCase.execute(dto);

        Assertions.assertTrue(created.getRoles().stream().anyMatch(r -> "DOCTOR".equals(r.getCode())));
        Assertions.assertEquals("new@statusscope.local", created.getEmail());
    }

    @Test
    void shouldFailIfDefaultRoleMissing() {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setFullName("Dr. NoRole");
        dto.setEmail("norole@statusscope.local");
        dto.setExternalAuthId("ext-norole");

        Mockito.when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
        Mockito.when(userRepository.findByExternalAuthId(dto.getExternalAuthId())).thenReturn(Optional.empty());
        Mockito.when(roleRepository.findByCode("DOCTOR")).thenReturn(Optional.empty());

        Assertions.assertThrows(NotFoundException.class, () -> useCase.execute(dto));
    }
}
