package com.itesm.application.usecase;

import com.itesm.application.dto.AssistantFeedbackDto;
import com.itesm.application.dto.DiagnosisEvaluationDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.infrastructure.persistence.entity.DiagnosisAssistantMessageEntity;
import com.itesm.infrastructure.persistence.entity.DiagnosisAssistantThreadEntity;
import com.itesm.infrastructure.persistence.entity.DiagnosisFeedbackEventEntity;
import com.itesm.infrastructure.persistence.entity.DiseaseEntity;
import com.itesm.infrastructure.persistence.entity.HospitalEntity;
import com.itesm.infrastructure.persistence.entity.PatientEvaluationEntity;
import com.itesm.infrastructure.persistence.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class RecordAssistantFeedbackUseCase {

    private static final Set<String> ALLOWED_DECISIONS = Set.of(
            "ASSISTANT_ACCEPTED",
            "ASSISTANT_REJECTED_DOCTOR_OVERRIDE",
            "DOCTOR_ONLY"
    );

    @Inject
    AuthenticatedUserContext authenticatedUserContext;

    @Inject
    EntityManager entityManager;

    @Inject
    GetDiagnosisEvaluationUseCase getDiagnosisEvaluationUseCase;

    @Transactional
    public DiagnosisEvaluationDto record(UUID evaluationId, AssistantFeedbackDto dto) {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        String decision = normalizeDecision(dto.getFinalDecisionSource());

        PatientEvaluationEntity evaluation = loadManagedEvaluation(evaluationId, currentUser.getUserId());

        DiseaseEntity finalDisease = resolveRequiredDisease(dto.getFinalDiseaseId());
        String finalLabel = finalDisease.getName();
        String notes = trimToNull(dto.getDoctorFeedbackNotes());
        LocalDateTime now = LocalDateTime.now();

        evaluation.setStatus(decisionToStatus(decision));
        evaluation.setFinalDecisionSource(decision);
        evaluation.setFinalDisease(finalDisease);
        evaluation.setFinalDiagnosisLabel(finalLabel);
        evaluation.setDoctorFeedbackNotes(notes);
        evaluation.setUpdatedAt(now);
        evaluation.setFinalizedAt(now);

        DiagnosisAssistantThreadEntity thread = findThread(evaluationId);
        DiagnosisAssistantMessageEntity acceptedMessage =
                resolveAcceptedMessage(dto.getAcceptedAssistantMessageId(), thread);

        DiagnosisFeedbackEventEntity event = new DiagnosisFeedbackEventEntity();
        event.setId(UUID.randomUUID());
        event.setEvaluation(evaluation);
        event.setThread(thread);
        event.setDoctor(entityManager.getReference(UserEntity.class, currentUser.getUserId()));
        event.setHospital(currentUser.getHospitalId() == null
                ? null
                : entityManager.getReference(HospitalEntity.class, currentUser.getHospitalId()));
        event.setFeedbackType(decision);
        event.setAcceptedAssistantMessage(acceptedMessage);
        event.setFinalDisease(finalDisease);
        event.setFinalDiagnosisLabel(finalLabel);
        event.setFeedbackNotes(notes);
        event.setCreatedAt(now);
        entityManager.persist(event);

        entityManager.flush();
        return getDiagnosisEvaluationUseCase.byId(evaluationId);
    }

    private PatientEvaluationEntity loadManagedEvaluation(UUID evaluationId, UUID doctorId) {
        return entityManager.createQuery("""
                select e
                from PatientEvaluationEntity e
                where e.id = :evaluationId
                  and e.doctor.id = :doctorId
                """, PatientEvaluationEntity.class)
                .setParameter("evaluationId", evaluationId)
                .setParameter("doctorId", doctorId)
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Diagnosis evaluation not found"));
    }

    private DiagnosisAssistantThreadEntity findThread(UUID evaluationId) {
        return entityManager.createQuery("""
                select t
                from DiagnosisAssistantThreadEntity t
                where t.evaluation.id = :evaluationId
                """, DiagnosisAssistantThreadEntity.class)
                .setParameter("evaluationId", evaluationId)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    private DiagnosisAssistantMessageEntity resolveAcceptedMessage(UUID messageId,
                                                                    DiagnosisAssistantThreadEntity thread) {
        if (messageId == null || thread == null) {
            return null;
        }
        DiagnosisAssistantMessageEntity message = entityManager.find(DiagnosisAssistantMessageEntity.class, messageId);
        if (message == null || !thread.getId().equals(message.getThread().getId())) {
            throw new IllegalArgumentException("Accepted assistant message does not belong to this evaluation");
        }
        if (!"assistant".equalsIgnoreCase(message.getRole())) {
            throw new IllegalArgumentException("Accepted message must be an assistant message");
        }
        return message;
    }

    private DiseaseEntity resolveRequiredDisease(UUID diseaseId) {
        if (diseaseId == null) {
            throw new IllegalArgumentException("finalDiseaseId is required");
        }
        DiseaseEntity disease = entityManager.find(DiseaseEntity.class, diseaseId);
        if (disease == null) {
            throw new NotFoundException("Disease not found for id: " + diseaseId);
        }
        return disease;
    }

    private String normalizeDecision(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_DECISIONS.contains(normalized)) {
            throw new IllegalArgumentException("Invalid finalDecisionSource");
        }
        return normalized;
    }

    private String decisionToStatus(String decision) {
        return "ASSISTANT_REJECTED_DOCTOR_OVERRIDE".equals(decision) ? "REJECTED" : "CONFIRMED";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
