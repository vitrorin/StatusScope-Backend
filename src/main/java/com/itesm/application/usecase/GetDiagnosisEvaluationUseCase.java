package com.itesm.application.usecase;

import com.itesm.application.dto.DiagnosisDifferentialDto;
import com.itesm.application.dto.DiagnosisEvaluationDto;
import com.itesm.application.dto.DiagnosisEvaluationEventDto;
import com.itesm.application.dto.DiagnosisEvaluationFileDto;
import com.itesm.application.dto.DiagnosisEvaluationPatientDto;
import com.itesm.application.dto.DiagnosisRecommendedTestDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.infrastructure.persistence.entity.EvaluationDifferentialDiagnosisEntity;
import com.itesm.infrastructure.persistence.entity.EvaluationRecommendedTestEntity;
import com.itesm.infrastructure.persistence.entity.EventEntity;
import com.itesm.infrastructure.persistence.entity.PatientEntity;
import com.itesm.infrastructure.persistence.entity.PatientEvaluationEntity;
import com.itesm.infrastructure.persistence.entity.PatientEvaluationFileEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class GetDiagnosisEvaluationUseCase {

    @Inject
    AuthenticatedUserContext authenticatedUserContext;

    @Inject
    EntityManager entityManager;

    public DiagnosisEvaluationDto current() {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        TypedQuery<PatientEvaluationEntity> query = entityManager.createQuery("""
                select e
                from PatientEvaluationEntity e
                join fetch e.patient p
                left join fetch e.event ev
                left join fetch ev.disease d
                where e.doctor.id = :doctorId
                  and e.status = 'IN_PROGRESS'
                order by e.createdAt desc
                """, PatientEvaluationEntity.class);
        query.setParameter("doctorId", currentUser.getUserId());
        query.setMaxResults(1);

        List<PatientEvaluationEntity> evaluations = query.getResultList();
        if (evaluations.isEmpty()) {
            throw new NotFoundException("No diagnosis evaluation found for current doctor");
        }

        return toDto(evaluations.get(0));
    }

    public DiagnosisEvaluationDto byId(UUID evaluationId) {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        TypedQuery<PatientEvaluationEntity> query = entityManager.createQuery("""
                select e
                from PatientEvaluationEntity e
                join fetch e.patient p
                left join fetch e.event ev
                left join fetch ev.disease d
                where e.id = :evaluationId
                  and e.doctor.id = :doctorId
                """, PatientEvaluationEntity.class);
        query.setParameter("evaluationId", evaluationId);
        query.setParameter("doctorId", currentUser.getUserId());

        List<PatientEvaluationEntity> evaluations = query.getResultList();
        if (evaluations.isEmpty()) {
            throw new NotFoundException("Diagnosis evaluation not found");
        }

        return toDto(evaluations.get(0));
    }

    private DiagnosisEvaluationDto toDto(PatientEvaluationEntity evaluation) {
        DiagnosisEvaluationDto dto = new DiagnosisEvaluationDto();
        dto.setId(evaluation.getId());
        dto.setStatus(evaluation.getStatus());
        dto.setSymptomsText(evaluation.getSymptomsText());
        dto.setClinicalNotes(evaluation.getClinicalNotes());
        dto.setCreatedAt(evaluation.getCreatedAt());
        dto.setUpdatedAt(evaluation.getUpdatedAt());
        dto.setFinalizedAt(evaluation.getFinalizedAt());
        dto.setFinalDiseaseId(evaluation.getFinalDisease() == null ? null : evaluation.getFinalDisease().getId());
        dto.setFinalDiseaseName(evaluation.getFinalDisease() == null ? null : evaluation.getFinalDisease().getName());
        dto.setFinalDiagnosisLabel(evaluation.getFinalDiagnosisLabel());
        dto.setFinalDecisionSource(evaluation.getFinalDecisionSource());
        dto.setDoctorFeedbackNotes(evaluation.getDoctorFeedbackNotes());
        dto.setPatient(toPatientDto(evaluation.getPatient()));
        dto.setEvent(toEventDto(evaluation.getEvent()));
        dto.setDifferentialDiagnoses(loadDifferentials(evaluation.getId()));
        dto.setRecommendedTests(loadRecommendedTests(evaluation.getId()));
        dto.setFiles(loadFiles(evaluation.getId()));
        return dto;
    }

    private DiagnosisEvaluationPatientDto toPatientDto(PatientEntity patient) {
        DiagnosisEvaluationPatientDto dto = new DiagnosisEvaluationPatientDto();
        dto.setId(patient.getId());
        dto.setFullName(patient.getFullName());
        dto.setSex(patient.getSex());
        dto.setBirthDate(patient.getBirthDate());
        dto.setAgeYears(ageYears(patient.getBirthDate()));
        dto.setWeightKg(patient.getWeightKg());
        dto.setHeightCm(patient.getHeightCm());
        dto.setPostalCode(patient.getPostalCode());
        return dto;
    }

    private DiagnosisEvaluationEventDto toEventDto(EventEntity event) {
        if (event == null) {
            return null;
        }

        DiagnosisEvaluationEventDto dto = new DiagnosisEvaluationEventDto();
        dto.setId(event.getId());
        dto.setStatus(event.getStatus());
        dto.setStartedAt(event.getStartedAt());
        if (event.getDisease() != null) {
            dto.setDiseaseName(event.getDisease().getName());
            dto.setDiseaseCode(event.getDisease().getCode());
        }
        return dto;
    }

    private List<DiagnosisDifferentialDto> loadDifferentials(UUID evaluationId) {
        return entityManager.createQuery("""
                select d
                from EvaluationDifferentialDiagnosisEntity d
                left join fetch d.disease disease
                where d.evaluation.id = :evaluationId
                order by d.rankOrder asc
                """, EvaluationDifferentialDiagnosisEntity.class)
                .setParameter("evaluationId", evaluationId)
                .getResultStream()
                .map(this::toDifferentialDto)
                .collect(Collectors.toList());
    }

    private DiagnosisDifferentialDto toDifferentialDto(EvaluationDifferentialDiagnosisEntity differential) {
        DiagnosisDifferentialDto dto = new DiagnosisDifferentialDto();
        dto.setId(differential.getId());
        dto.setDisplayName(differential.getDisplayName());
        dto.setConfidence(differential.getConfidence());
        dto.setRationale(differential.getRationale());
        dto.setRankOrder(differential.getRankOrder());
        dto.setLocalityRiskLevel(differential.getLocalityRiskLevel());
        if (differential.getDisease() != null) {
            dto.setDiseaseId(differential.getDisease().getId());
            dto.setDiseaseCode(differential.getDisease().getCode());
        }
        return dto;
    }

    private List<DiagnosisRecommendedTestDto> loadRecommendedTests(UUID evaluationId) {
        return entityManager.createQuery("""
                select t
                from EvaluationRecommendedTestEntity t
                where t.evaluation.id = :evaluationId
                order by t.sortOrder asc
                """, EvaluationRecommendedTestEntity.class)
                .setParameter("evaluationId", evaluationId)
                .getResultStream()
                .map(this::toRecommendedTestDto)
                .collect(Collectors.toList());
    }

    private DiagnosisRecommendedTestDto toRecommendedTestDto(EvaluationRecommendedTestEntity test) {
        DiagnosisRecommendedTestDto dto = new DiagnosisRecommendedTestDto();
        dto.setId(test.getId());
        dto.setTestName(test.getTestName());
        dto.setReason(test.getReason());
        dto.setSource(test.getSource());
        dto.setSortOrder(test.getSortOrder());
        return dto;
    }

    private List<DiagnosisEvaluationFileDto> loadFiles(UUID evaluationId) {
        return entityManager.createQuery("""
                select f
                from PatientEvaluationFileEntity f
                where f.evaluation.id = :evaluationId
                order by f.uploadedAt desc
                """, PatientEvaluationFileEntity.class)
                .setParameter("evaluationId", evaluationId)
                .getResultStream()
                .map(this::toFileDto)
                .collect(Collectors.toList());
    }

    private DiagnosisEvaluationFileDto toFileDto(PatientEvaluationFileEntity file) {
        DiagnosisEvaluationFileDto dto = new DiagnosisEvaluationFileDto();
        dto.setId(file.getId());
        dto.setFileName(file.getFileName());
        dto.setMimeType(file.getMimeType());
        dto.setStorageKey(file.getStorageKey());
        dto.setFileSizeBytes(file.getFileSizeBytes());
        dto.setDocumentType(file.getDocumentType());
        dto.setUploadedAt(file.getUploadedAt());
        return dto;
    }

    private Integer ageYears(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
