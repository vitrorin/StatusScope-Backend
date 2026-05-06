package com.itesm.application.usecase;

import com.itesm.application.dto.AssistantContextDto;
import com.itesm.application.dto.AssistantMessageDto;
import com.itesm.application.dto.AssistantRequestDto;
import com.itesm.application.dto.AssistantResponseDto;
import com.itesm.application.dto.OutbreakSummaryDto;
import com.itesm.application.port.out.AssistantChatGateway;
import com.itesm.application.port.out.AssistantChatMessage;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.Hospital;
import com.itesm.domain.models.Outbreak;
import com.itesm.domain.models.Region;
import com.itesm.domain.repository.HospitalRepository;
import com.itesm.domain.repository.OutbreakRepository;
import com.itesm.infrastructure.persistence.entity.DiagnosisAssistantMessageEntity;
import com.itesm.infrastructure.persistence.entity.DiagnosisAssistantThreadEntity;
import com.itesm.infrastructure.persistence.entity.HospitalEntity;
import com.itesm.infrastructure.persistence.entity.PatientEvaluationEntity;
import com.itesm.infrastructure.persistence.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class AskDiagnosisAssistantUseCase {

    @Inject
    AuthenticatedUserContext authenticatedUserContext;

    @Inject
    HospitalRepository hospitalRepository;

    @Inject
    OutbreakRepository outbreakRepository;

    @Inject
    AssistantChatGateway assistantChatGateway;

    @Inject
    AssistantPromptBuilder promptBuilder;

    @Inject
    EntityManager entityManager;

    @Transactional
    public AssistantResponseDto execute(AssistantRequestDto request) {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();

        UUID hospitalId = currentUser.getHospitalId();
        if (hospitalId == null) {
            throw new NotFoundException("Doctor has no assigned hospital");
        }

        Hospital hospital = hospitalRepository.findHospitalById(hospitalId)
                .orElseThrow(() -> new NotFoundException("Hospital not found for id: " + hospitalId));

        UUID regionId = hospital.getRegionId();
        List<Outbreak> outbreaks = new ArrayList<>();
        Region region = null;

        if (regionId != null) {
            outbreaks = outbreakRepository.findActiveByRegionId(regionId);
            if (!outbreaks.isEmpty() && outbreaks.get(0).getRegion() != null) {
                region = outbreaks.get(0).getRegion();
            }
        }

        String systemPrompt = promptBuilder.build(region, outbreaks, request.getPatientContext());

        AssistantMessageDto latestUserMessage = latestUserMessage(request.getMessages());
        DiagnosisAssistantThreadEntity thread = null;
        List<AssistantMessageDto> conversationHistory = new ArrayList<>();

        if (request.getEvaluationId() != null) {
            PatientEvaluationEntity evaluation = loadManagedEvaluation(request.getEvaluationId(), currentUser.getUserId());
            thread = findOrCreateThread(evaluation, currentUser);
            conversationHistory.addAll(loadThreadMessages(thread.getId()));

            if (latestUserMessage != null) {
                DiagnosisAssistantMessageEntity persistedUserMessage = persistMessage(
                        thread,
                        "user",
                        latestUserMessage.getContent(),
                        conversationHistory.size() + 1);
                conversationHistory.add(toAssistantMessageDto(persistedUserMessage));
            }
        } else {
            conversationHistory.addAll(request.getMessages());
        }

        List<AssistantChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new AssistantChatMessage("system", systemPrompt));
        for (AssistantMessageDto msg : conversationHistory) {
            chatMessages.add(new AssistantChatMessage(msg.getRole(), msg.getContent()));
        }

        String reply = assistantChatGateway.chat(chatMessages);

        if (thread != null) {
            persistMessage(thread, "assistant", reply, conversationHistory.size() + 1);
        }

        List<OutbreakSummaryDto> outbreakSummaries = outbreaks.stream()
                .filter(o -> o.getDisease() != null)
                .map(o -> new OutbreakSummaryDto(
                        o.getDisease().getName(),
                        o.getCaseCount(),
                        o.getStartedAt()))
                .collect(Collectors.toList());

        String regionName = region != null ? region.getName() : null;
        AssistantContextDto contextUsed = new AssistantContextDto(regionName, outbreakSummaries);

        return new AssistantResponseDto(reply, contextUsed);
    }

    private AssistantMessageDto latestUserMessage(List<AssistantMessageDto> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        AssistantMessageDto latest = messages.get(messages.size() - 1);
        if (!"user".equalsIgnoreCase(latest.getRole())) {
            throw new IllegalArgumentException("Latest assistant message payload must be from the user");
        }
        return latest;
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

    private DiagnosisAssistantThreadEntity findOrCreateThread(PatientEvaluationEntity evaluation, CurrentUser currentUser) {
        DiagnosisAssistantThreadEntity existing = entityManager.createQuery("""
                select t
                from DiagnosisAssistantThreadEntity t
                where t.evaluation.id = :evaluationId
                """, DiagnosisAssistantThreadEntity.class)
                .setParameter("evaluationId", evaluation.getId())
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (existing != null) {
            return existing;
        }

        LocalDateTime now = LocalDateTime.now();
        DiagnosisAssistantThreadEntity thread = new DiagnosisAssistantThreadEntity();
        thread.setId(UUID.randomUUID());
        thread.setEvaluation(evaluation);
        thread.setDoctor(entityManager.getReference(UserEntity.class, currentUser.getUserId()));
        thread.setHospital(currentUser.getHospitalId() == null
                ? null
                : entityManager.getReference(HospitalEntity.class, currentUser.getHospitalId()));
        thread.setStatus("OPEN");
        thread.setCreatedAt(now);
        thread.setUpdatedAt(now);
        entityManager.persist(thread);
        return thread;
    }

    private List<AssistantMessageDto> loadThreadMessages(UUID threadId) {
        return entityManager.createQuery("""
                select m
                from DiagnosisAssistantMessageEntity m
                where m.thread.id = :threadId
                order by m.sequenceNo asc
                """, DiagnosisAssistantMessageEntity.class)
                .setParameter("threadId", threadId)
                .getResultStream()
                .map(this::toAssistantMessageDto)
                .collect(Collectors.toList());
    }

    private DiagnosisAssistantMessageEntity persistMessage(
            DiagnosisAssistantThreadEntity thread,
            String role,
            String content,
            int sequenceNo
    ) {
        DiagnosisAssistantMessageEntity message = new DiagnosisAssistantMessageEntity();
        message.setId(UUID.randomUUID());
        message.setThread(thread);
        message.setRole(role.trim().toLowerCase(Locale.ROOT));
        message.setMessageText(content.trim());
        message.setSequenceNo(sequenceNo);
        message.setCreatedAt(LocalDateTime.now());
        entityManager.persist(message);

        thread.setUpdatedAt(message.getCreatedAt());
        return message;
    }

    private AssistantMessageDto toAssistantMessageDto(DiagnosisAssistantMessageEntity message) {
        AssistantMessageDto dto = new AssistantMessageDto();
        dto.setRole(message.getRole());
        dto.setContent(message.getMessageText());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }
}
