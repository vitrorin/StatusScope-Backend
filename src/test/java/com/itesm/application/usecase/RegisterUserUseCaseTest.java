package com.itesm.application.usecase;

import com.google.firebase.auth.FirebaseAuthException;
import com.itesm.application.dto.RegisterUserDto;
import com.itesm.application.dto.UserSummaryDto;
import com.itesm.application.usecase.exception.ConflictException;
import com.itesm.application.usecase.exception.InvalidInviteException;
import com.itesm.domain.models.Hospital;
import com.itesm.domain.models.Role;
import com.itesm.domain.models.User;
import com.itesm.domain.repository.HospitalRepository;
import com.itesm.domain.repository.RoleRepository;
import com.itesm.domain.repository.UserRepository;
import com.itesm.infrastructure.firebase.FirebaseUserService;
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
    private HospitalRepository hospitalRepository;
    private FirebaseUserService firebaseUserService;

    @BeforeEach
    void setUp() throws FirebaseAuthException {
        useCase = new RegisterUserUseCase();
        userRepository = Mockito.mock(UserRepository.class);
        roleRepository = Mockito.mock(RoleRepository.class);
        hospitalRepository = Mockito.mock(HospitalRepository.class);
        firebaseUserService = Mockito.mock(FirebaseUserService.class);
        useCase.userRepository = userRepository;
        useCase.roleRepository = roleRepository;
        useCase.hospitalRepository = hospitalRepository;
        useCase.firebaseUserService = firebaseUserService;

        Mockito.when(firebaseUserService.createUser(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn("mock-firebase-uid");
    }

    @Test
    void shouldRegisterUserWithValidInviteCode() throws FirebaseAuthException {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setFullName("Dr. New");
        dto.setEmail("new@statusscope.local");
        dto.setPassword("Password123!");
        dto.setInviteCode("VALID-CODE");

        Hospital hospital = new Hospital();
        hospital.setId(UUID.randomUUID());
        hospital.setCode("HGZ-21");
        hospital.setName("IMSS HGZ 21");
        hospital.setActive(true);
        hospital.setInviteCode("VALID-CODE");

        Role doctor = new Role();
        doctor.setId(UUID.randomUUID());
        doctor.setCode("DOCTOR");

        Mockito.when(hospitalRepository.findByInviteCode("VALID-CODE")).thenReturn(Optional.of(hospital));
        Mockito.when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
        Mockito.when(roleRepository.findByCode("DOCTOR")).thenReturn(Optional.of(doctor));
        Mockito.when(userRepository.create(Mockito.any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserSummaryDto result = useCase.execute(dto);

        Assertions.assertEquals("new@statusscope.local", result.getEmail());
        Assertions.assertTrue(result.getRoles().contains("DOCTOR"));
        Assertions.assertEquals(hospital.getId(), result.getHospitalId());
    }

    @Test
    void shouldThrowInvalidInviteWhenCodeNotFound() {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setFullName("X");
        dto.setEmail("x@statusscope.local");
        dto.setPassword("Password123!");
        dto.setInviteCode("WRONG-CODE");

        Mockito.when(hospitalRepository.findByInviteCode("WRONG-CODE")).thenReturn(Optional.empty());

        Assertions.assertThrows(InvalidInviteException.class, () -> useCase.execute(dto));
    }

    @Test
    void shouldThrowConflictWhenEmailAlreadyExists() {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setFullName("Duplicate");
        dto.setEmail("dup@statusscope.local");
        dto.setPassword("Password123!");
        dto.setInviteCode("VALID-CODE");

        Hospital hospital = new Hospital();
        hospital.setId(UUID.randomUUID());
        hospital.setActive(true);
        hospital.setInviteCode("VALID-CODE");

        Mockito.when(hospitalRepository.findByInviteCode("VALID-CODE")).thenReturn(Optional.of(hospital));
        Mockito.when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(new User()));

        Assertions.assertThrows(ConflictException.class, () -> useCase.execute(dto));
    }
}

