package com.itesm.application.usecase;

import com.itesm.application.dto.DiagnosisEvaluationDto;
import com.itesm.application.dto.UploadDiagnosisEvaluationFileDto;
import com.itesm.application.dto.UpdateDiagnosisEvaluationDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.infrastructure.persistence.entity.PatientEntity;
import com.itesm.infrastructure.persistence.entity.PatientEvaluationEntity;
import com.itesm.infrastructure.persistence.entity.PatientEvaluationFileEntity;
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
public class UpdateDiagnosisEvaluationUseCase {

    private static final Set<String> ALLOWED_SEXES = Set.of("male", "female", "other");
    private static final Set<String> ALLOWED_STATUSES = Set.of("IN_PROGRESS", "CONFIRMED", "REJECTED");

    @Inject
    AuthenticatedUserContext authenticatedUserContext;

    @Inject
    EntityManager entityManager;

    @Inject
    GetDiagnosisEvaluationUseCase getDiagnosisEvaluationUseCase;

    @Transactional
    public DiagnosisEvaluationDto update(UUID evaluationId, UpdateDiagnosisEvaluationDto dto) {
        PatientEvaluationEntity evaluation = loadManagedEvaluation(evaluationId);
        PatientEntity patient = evaluation.getPatient();
        LocalDateTime now = LocalDateTime.now();

        patient.setFullName(dto.getPatientFullName().trim());
        patient.setSex(normalizeSex(dto.getSex()));
        patient.setBirthDate(parseBirthDate(dto.getBirthDate()));
        patient.setUpdatedAt(now);

        evaluation.setSymptomsText(dto.getSymptomsText().trim());
        evaluation.setUpdatedAt(now);

        entityManager.flush();
        return getDiagnosisEvaluationUseCase.byId(evaluationId);
    }

    @Transactional
    public DiagnosisEvaluationDto updateStatus(UUID evaluationId, String requestedStatus) {
        PatientEvaluationEntity evaluation = loadManagedEvaluation(evaluationId);
        String normalizedStatus = normalizeStatus(requestedStatus);
        LocalDateTime now = LocalDateTime.now();

        evaluation.setStatus(normalizedStatus);
        evaluation.setUpdatedAt(now);
        evaluation.setFinalizedAt("IN_PROGRESS".equals(normalizedStatus) ? null : now);

        entityManager.flush();
        return getDiagnosisEvaluationUseCase.byId(evaluationId);
    }

    @Transactional
    public DiagnosisEvaluationDto uploadFile(UUID evaluationId, UploadDiagnosisEvaluationFileDto dto) {
        PatientEvaluationEntity evaluation = loadManagedEvaluation(evaluationId);

        PatientEvaluationFileEntity file = new PatientEvaluationFileEntity();
        file.setId(UUID.randomUUID());
        file.setEvaluation(evaluation);
        file.setFileName(dto.getFileName().trim());
        file.setMimeType(dto.getMimeType().trim());
        file.setFileSizeBytes(dto.getFileSizeBytes());
        String documentType = dto.getDocumentType() == null || dto.getDocumentType().trim().isEmpty()
                ? "LAB_RESULT"
                : dto.getDocumentType().trim();
        file.setDocumentType(documentType);
        file.setContentBase64(dto.getContentBase64().trim());
        file.setStorageKey("db:" + file.getId());
        file.setUploadedAt(LocalDateTime.now());
        entityManager.persist(file);

        evaluation.setUpdatedAt(LocalDateTime.now());
        entityManager.flush();
        return getDiagnosisEvaluationUseCase.byId(evaluationId);
    }

    private PatientEvaluationEntity loadManagedEvaluation(UUID evaluationId) {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        var query = entityManager.createQuery("""
                select e
                from PatientEvaluationEntity e
                join fetch e.patient p
                where e.id = :evaluationId
                  and e.doctor.id = :doctorId
                """, PatientEvaluationEntity.class);
        query.setParameter("evaluationId", evaluationId);
        query.setParameter("doctorId", currentUser.getUserId());
        return query.getResultStream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Diagnosis evaluation not found"));
    }

    private String normalizeSex(String sex) {
        String normalized = sex.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_SEXES.contains(normalized)) {
            throw new IllegalArgumentException("Invalid sex value");
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Invalid evaluation status");
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
