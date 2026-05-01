package com.itesm.application.usecase;

import com.itesm.application.dto.DiagnosisEvaluationDto;
import com.itesm.application.dto.UpdateDiagnosisEvaluationDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.ConflictException;
import com.itesm.infrastructure.persistence.entity.HospitalEntity;
import com.itesm.infrastructure.persistence.entity.PatientEntity;
import com.itesm.infrastructure.persistence.entity.PatientEvaluationEntity;
import com.itesm.infrastructure.persistence.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class CreateDiagnosisEvaluationUseCase {

    private static final Set<String> ALLOWED_SEXES = Set.of("male", "female", "other");

    @Inject
    AuthenticatedUserContext authenticatedUserContext;

    @Inject
    EntityManager entityManager;

    @Inject
    GetDiagnosisEvaluationUseCase getDiagnosisEvaluationUseCase;

    @Transactional
    public DiagnosisEvaluationDto create(UpdateDiagnosisEvaluationDto dto) {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        if (currentUser.getHospitalId() == null) {
            throw new ConflictException("Current user is not assigned to a hospital");
        }

        LocalDateTime now = LocalDateTime.now();
        HospitalEntity hospital = entityManager.getReference(HospitalEntity.class, currentUser.getHospitalId());
        UserEntity doctor = entityManager.getReference(UserEntity.class, currentUser.getUserId());

        PatientEntity patient = new PatientEntity();
        patient.setId(UUID.randomUUID());
        patient.setHospital(hospital);
        patient.setFullName(dto.getPatientFullName().trim());
        patient.setSex(normalizeSex(dto.getSex()));
        patient.setBirthDate(parseBirthDate(dto.getBirthDate()));
        patient.setCreatedAt(now);
        patient.setUpdatedAt(now);
        entityManager.persist(patient);

        PatientEvaluationEntity evaluation = new PatientEvaluationEntity();
        evaluation.setId(UUID.randomUUID());
        evaluation.setPatient(patient);
        evaluation.setDoctor(doctor);
        evaluation.setStatus("IN_PROGRESS");
        evaluation.setSymptomsText(dto.getSymptomsText().trim());
        evaluation.setCreatedAt(now);
        evaluation.setUpdatedAt(now);
        entityManager.persist(evaluation);

        entityManager.flush();
        return getDiagnosisEvaluationUseCase.byId(evaluation.getId());
    }

    private String normalizeSex(String sex) {
        String normalized = sex.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_SEXES.contains(normalized)) {
            throw new IllegalArgumentException("Invalid sex value");
        }
        return normalized;
    }

    private LocalDate parseBirthDate(String birthDate) {
        try {
            LocalDate parsed = LocalDate.parse(birthDate.trim());
            if (parsed.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Birth date cannot be in the future");
            }
            return parsed;
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null && exception.getMessage().contains("future")) {
                throw exception;
            }
            throw new IllegalArgumentException("Birth date must use YYYY-MM-DD format");
        }
    }
}
