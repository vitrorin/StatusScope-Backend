package com.itesm.application.usecase;

import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.ConflictException;
import com.itesm.infrastructure.persistence.entity.HospitalEntity;
import com.itesm.infrastructure.persistence.entity.PatientEntity;
import com.itesm.infrastructure.persistence.entity.PatientEvaluationEntity;
import com.itesm.infrastructure.persistence.entity.UserEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import com.itesm.application.dto.UpdateDiagnosisEvaluationDto;
import com.itesm.application.dto.DiagnosisEvaluationDto;
import com.itesm.application.dto.DiagnosisEvaluationPatientDto;

class CreateDiagnosisEvaluationUseCaseTest {

    private static final UUID HOSPITAL_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID DOCTOR_ID = UUID.fromString("11000000-0000-0000-0000-000000000001");

    private CreateDiagnosisEvaluationUseCase useCase;
    private AuthenticatedUserContext authenticatedUserContext;
    private EntityManager entityManager;
    private GetDiagnosisEvaluationUseCase getDiagnosisEvaluationUseCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateDiagnosisEvaluationUseCase();
        authenticatedUserContext = Mockito.mock(AuthenticatedUserContext.class);
        entityManager = Mockito.mock(EntityManager.class);
        getDiagnosisEvaluationUseCase = Mockito.mock(GetDiagnosisEvaluationUseCase.class);

        useCase.authenticatedUserContext = authenticatedUserContext;
        useCase.entityManager = entityManager;
        useCase.getDiagnosisEvaluationUseCase = getDiagnosisEvaluationUseCase;

        CurrentUser currentUser = new CurrentUser(
                DOCTOR_ID, "ext-id", "doctor@test.local", "Dr. Test",
                HOSPITAL_ID, Set.of("DOCTOR"), Set.of("diagnosis.write"));
        Mockito.when(authenticatedUserContext.getCurrentUser()).thenReturn(currentUser);

        Mockito.when(entityManager.getReference(HospitalEntity.class, HOSPITAL_ID))
                .thenReturn(new HospitalEntity());
        Mockito.when(entityManager.getReference(UserEntity.class, DOCTOR_ID))
                .thenReturn(new UserEntity());

        DiagnosisEvaluationDto dto = new DiagnosisEvaluationDto();
        dto.setId(UUID.randomUUID());
        dto.setStatus("IN_PROGRESS");
        DiagnosisEvaluationPatientDto patient = new DiagnosisEvaluationPatientDto();
        patient.setFullName("Test Patient");
        patient.setSex("male");
        patient.setBirthDate(LocalDate.of(1990, 1, 1));
        dto.setPatient(patient);
        Mockito.when(getDiagnosisEvaluationUseCase.byId(Mockito.any())).thenReturn(dto);
    }

    private UpdateDiagnosisEvaluationDto buildDto(String name, String sex, String birthDate, String symptoms) {
        UpdateDiagnosisEvaluationDto dto = new UpdateDiagnosisEvaluationDto();
        dto.setPatientFullName(name);
        dto.setSex(sex);
        dto.setBirthDate(birthDate);
        dto.setSymptomsText(symptoms);
        return dto;
    }

    // ── Hospital assignment guard ─────────────────────────────────────────────

    @Test
    void shouldThrowConflictWhenUserHasNoHospital() {
        CurrentUser userWithoutHospital = new CurrentUser(
                DOCTOR_ID, "ext-id", "doctor@test.local", "Dr. Test",
                null, Set.of("DOCTOR"), Set.of("diagnosis.write"));
        Mockito.when(authenticatedUserContext.getCurrentUser()).thenReturn(userWithoutHospital);

        Assertions.assertThrows(ConflictException.class,
                () -> useCase.create(buildDto("John", "male", "1990-01-01", "Fever")));
    }

    // ── Sex normalization ─────────────────────────────────────────────────────

    @Test
    void shouldNormalizeSexToLowerCase() {
        useCase.create(buildDto("John", "MALE", "1990-01-01", "Fever"));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(entityManager, Mockito.atLeastOnce()).persist(captor.capture());

        PatientEntity patient = captor.getAllValues().stream()
                .filter(o -> o instanceof PatientEntity)
                .map(o -> (PatientEntity) o)
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals("male", patient.getSex());
    }

    @Test
    void shouldAcceptFemaleValue() {
        useCase.create(buildDto("Jane", "female", "1990-01-01", "Cough"));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(entityManager, Mockito.atLeastOnce()).persist(captor.capture());

        PatientEntity patient = captor.getAllValues().stream()
                .filter(o -> o instanceof PatientEntity)
                .map(o -> (PatientEntity) o)
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals("female", patient.getSex());
    }

    @Test
    void shouldAcceptOtherSexValue() {
        useCase.create(buildDto("Alex", "other", "1990-01-01", "Rash"));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(entityManager, Mockito.atLeastOnce()).persist(captor.capture());

        PatientEntity patient = captor.getAllValues().stream()
                .filter(o -> o instanceof PatientEntity)
                .map(o -> (PatientEntity) o)
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals("other", patient.getSex());
    }

    @Test
    void shouldThrowForInvalidSexValue() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> useCase.create(buildDto("John", "unknown", "1990-01-01", "Fever")));
    }

    @Test
    void shouldThrowForEmptySexValue() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> useCase.create(buildDto("John", "", "1990-01-01", "Fever")));
    }

    // ── Birth date parsing ────────────────────────────────────────────────────

    @Test
    void shouldParseBirthDateCorrectly() {
        useCase.create(buildDto("John", "male", "1985-06-15", "Fever"));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(entityManager, Mockito.atLeastOnce()).persist(captor.capture());

        PatientEntity patient = captor.getAllValues().stream()
                .filter(o -> o instanceof PatientEntity)
                .map(o -> (PatientEntity) o)
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals(LocalDate.of(1985, 6, 15), patient.getBirthDate());
    }

    @Test
    void shouldThrowForFutureBirthDate() {
        String future = LocalDate.now().plusDays(1).toString();
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> useCase.create(buildDto("John", "male", future, "Fever")));
    }

    @Test
    void shouldThrowForInvalidDateFormat() {
        // LocalDate.parse throws DateTimeParseException (a RuntimeException) for bad formats
        Assertions.assertThrows(RuntimeException.class,
                () -> useCase.create(buildDto("John", "male", "15/06/1985", "Fever")));
    }

    @Test
    void shouldThrowForNonExistentDate() {
        // LocalDate.parse throws DateTimeParseException for non-existent dates
        Assertions.assertThrows(RuntimeException.class,
                () -> useCase.create(buildDto("John", "male", "1985-13-01", "Fever")));
    }

    // ── Today's birth date is valid ───────────────────────────────────────────

    @Test
    void shouldAcceptTodayAsBirthDate() {
        String today = LocalDate.now().toString();
        // Should NOT throw
        Assertions.assertDoesNotThrow(
                () -> useCase.create(buildDto("Newborn", "female", today, "Fever")));
    }

    // ── Name trimming ─────────────────────────────────────────────────────────

    @Test
    void shouldTrimPatientName() {
        useCase.create(buildDto("  John Doe  ", "male", "1990-01-01", "Fever"));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(entityManager, Mockito.atLeastOnce()).persist(captor.capture());

        PatientEntity patient = captor.getAllValues().stream()
                .filter(o -> o instanceof PatientEntity)
                .map(o -> (PatientEntity) o)
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals("John Doe", patient.getFullName());
    }

    // ── Symptoms trimming ─────────────────────────────────────────────────────

    @Test
    void shouldTrimSymptomsText() {
        useCase.create(buildDto("John", "male", "1990-01-01", "  Fever and rash  "));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(entityManager, Mockito.atLeastOnce()).persist(captor.capture());

        PatientEvaluationEntity evaluation = captor.getAllValues().stream()
                .filter(o -> o instanceof PatientEvaluationEntity)
                .map(o -> (PatientEvaluationEntity) o)
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals("Fever and rash", evaluation.getSymptomsText());
    }

    // ── Evaluation status ─────────────────────────────────────────────────────

    @Test
    void shouldCreateEvaluationWithInProgressStatus() {
        useCase.create(buildDto("John", "male", "1990-01-01", "Fever"));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(entityManager, Mockito.atLeastOnce()).persist(captor.capture());

        PatientEvaluationEntity evaluation = captor.getAllValues().stream()
                .filter(o -> o instanceof PatientEvaluationEntity)
                .map(o -> (PatientEvaluationEntity) o)
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals("IN_PROGRESS", evaluation.getStatus());
    }

    // ── Persist both entities ─────────────────────────────────────────────────

    @Test
    void shouldPersistBothPatientAndEvaluation() {
        useCase.create(buildDto("John", "male", "1990-01-01", "Fever"));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(entityManager, Mockito.atLeastOnce()).persist(captor.capture());

        long patientCount = captor.getAllValues().stream()
                .filter(o -> o instanceof PatientEntity).count();
        long evalCount = captor.getAllValues().stream()
                .filter(o -> o instanceof PatientEvaluationEntity).count();

        Assertions.assertEquals(1, patientCount);
        Assertions.assertEquals(1, evalCount);
    }

    // ── Return value ──────────────────────────────────────────────────────────

    @Test
    void shouldReturnEvaluationDto() {
        DiagnosisEvaluationDto result = useCase.create(buildDto("John", "male", "1990-01-01", "Fever"));
        Assertions.assertNotNull(result);
        Assertions.assertEquals("IN_PROGRESS", result.getStatus());
    }
}
