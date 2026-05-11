package com.itesm.application.usecase;

import com.itesm.application.dto.AssistantContextDto;
import com.itesm.application.dto.AssistantMessageDto;
import com.itesm.application.dto.AssistantSuggestionDto;
import com.itesm.application.dto.AssistantThreadDto;
import com.itesm.application.dto.OutbreakSummaryDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.Hospital;
import com.itesm.domain.models.Outbreak;
import com.itesm.domain.models.Region;
import com.itesm.domain.repository.HospitalRepository;
import com.itesm.domain.repository.OutbreakRepository;
import com.itesm.infrastructure.persistence.entity.DiagnosisAssistantMessageEntity;
import com.itesm.infrastructure.persistence.entity.DiagnosisAssistantSuggestionEntity;
import com.itesm.infrastructure.persistence.entity.DiagnosisAssistantThreadEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class GetDiagnosisAssistantThreadUseCase {

    @Inject
    AuthenticatedUserContext authenticatedUserContext;

    @Inject
    HospitalRepository hospitalRepository;

    @Inject
    OutbreakRepository outbreakRepository;

    @Inject
    EntityManager entityManager;

    public AssistantThreadDto byEvaluationId(UUID evaluationId) {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();

        DiagnosisAssistantThreadEntity thread = entityManager.createQuery("""
                select t
                from DiagnosisAssistantThreadEntity t
                join fetch t.evaluation e
                where e.id = :evaluationId
                  and t.doctor.id = :doctorId
                """, DiagnosisAssistantThreadEntity.class)
                .setParameter("evaluationId", evaluationId)
                .setParameter("doctorId", currentUser.getUserId())
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Diagnosis assistant thread not found"));

        AssistantThreadDto dto = new AssistantThreadDto();
        dto.setId(thread.getId());
        dto.setEvaluationId(thread.getEvaluation().getId());
        dto.setCreatedAt(thread.getCreatedAt());
        dto.setUpdatedAt(thread.getUpdatedAt());
        dto.setMessages(loadMessages(thread.getId()));
        dto.setContextUsed(buildContextForCurrentDoctor(currentUser));
        return dto;
    }

    private List<AssistantMessageDto> loadMessages(UUID threadId) {
        List<DiagnosisAssistantMessageEntity> messages = entityManager.createQuery("""
                select m
                from DiagnosisAssistantMessageEntity m
                where m.thread.id = :threadId
                order by m.sequenceNo asc
                """, DiagnosisAssistantMessageEntity.class)
                .setParameter("threadId", threadId)
                .getResultList();

        if (messages.isEmpty()) {
            return List.of();
        }

        Map<UUID, List<AssistantSuggestionDto>> suggestionsByMessage = loadSuggestionsByMessage(
                messages.stream().map(DiagnosisAssistantMessageEntity::getId).collect(Collectors.toList()));

        return messages.stream()
                .map(m -> toMessageDto(m, suggestionsByMessage.getOrDefault(m.getId(), List.of())))
                .collect(Collectors.toList());
    }

    private Map<UUID, List<AssistantSuggestionDto>> loadSuggestionsByMessage(List<UUID> messageIds) {
        if (messageIds.isEmpty()) {
            return Map.of();
        }
        List<DiagnosisAssistantSuggestionEntity> rows = entityManager.createQuery("""
                select s
                from DiagnosisAssistantSuggestionEntity s
                left join fetch s.disease
                where s.message.id in :messageIds
                order by s.message.id, s.rankOrder asc
                """, DiagnosisAssistantSuggestionEntity.class)
                .setParameter("messageIds", messageIds)
                .getResultList();

        Map<UUID, List<AssistantSuggestionDto>> grouped = new HashMap<>();
        for (DiagnosisAssistantSuggestionEntity row : rows) {
            grouped.computeIfAbsent(row.getMessage().getId(), k -> new ArrayList<>()).add(toSuggestionDto(row));
        }
        return grouped;
    }

    private AssistantSuggestionDto toSuggestionDto(DiagnosisAssistantSuggestionEntity row) {
        AssistantSuggestionDto dto = new AssistantSuggestionDto();
        dto.setId(row.getId());
        dto.setMessageId(row.getMessage().getId());
        dto.setDiseaseId(row.getDisease() == null ? null : row.getDisease().getId());
        dto.setDisplayName(row.getDisplayName());
        dto.setRankOrder(row.getRankOrder());
        dto.setConfidence(row.getConfidence());
        dto.setRationale(row.getRationale());
        dto.setLocalityRiskLevel(row.getLocalityRiskLevel());
        dto.setPrimary(row.isWasPrimarySuggestion());
        return dto;
    }

    private AssistantMessageDto toMessageDto(DiagnosisAssistantMessageEntity message,
                                              List<AssistantSuggestionDto> suggestions) {
        AssistantMessageDto dto = new AssistantMessageDto();
        dto.setId(message.getId());
        dto.setRole(message.getRole());
        dto.setContent(message.getMessageText());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setSuggestions(suggestions);
        return dto;
    }

    private AssistantContextDto buildContextForCurrentDoctor(CurrentUser currentUser) {
        UUID hospitalId = currentUser.getHospitalId();
        if (hospitalId == null) {
            return new AssistantContextDto(null, List.of());
        }

        Hospital hospital = hospitalRepository.findHospitalById(hospitalId)
                .orElseThrow(() -> new NotFoundException("Hospital not found for id: " + hospitalId));

        UUID regionId = hospital.getRegionId();
        if (regionId == null) {
            return new AssistantContextDto(null, List.of());
        }

        List<Outbreak> outbreaks = outbreakRepository.findActiveByRegionId(regionId);
        Region region = outbreaks.isEmpty() ? null : outbreaks.get(0).getRegion();

        List<OutbreakSummaryDto> outbreakSummaries = outbreaks.stream()
                .filter(o -> o.getDisease() != null)
                .map(o -> new OutbreakSummaryDto(
                        o.getDisease().getName(),
                        o.getCaseCount(),
                        o.getStartedAt()))
                .collect(Collectors.toList());

        return new AssistantContextDto(region != null ? region.getName() : null, outbreakSummaries);
    }
}
