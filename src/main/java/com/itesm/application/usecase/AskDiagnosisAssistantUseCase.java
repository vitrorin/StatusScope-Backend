package com.itesm.application.usecase;

import com.itesm.application.dto.AssistantContextDto;
import com.itesm.application.dto.AssistantMessageDto;
import com.itesm.application.dto.AssistantRequestDto;
import com.itesm.application.dto.AssistantResponseDto;
import com.itesm.application.dto.AssistantSuggestionDto;
import com.itesm.application.dto.HospitalGeoContextDto;
import com.itesm.application.dto.OutbreakSummaryDto;
import com.itesm.application.port.out.AssistantChatGateway;
import com.itesm.application.port.out.AssistantChatMessage;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.Hospital;
import com.itesm.domain.models.Outbreak;
import com.itesm.domain.models.State;
import com.itesm.domain.repository.HospitalRepository;
import com.itesm.domain.repository.OutbreakRepository;
import com.itesm.infrastructure.persistence.entity.DiagnosisAssistantMessageEntity;
import com.itesm.infrastructure.persistence.entity.DiagnosisAssistantRetrievedCaseEntity;
import com.itesm.infrastructure.persistence.entity.DiagnosisAssistantSuggestionEntity;
import com.itesm.infrastructure.persistence.entity.DiagnosisAssistantThreadEntity;
import com.itesm.infrastructure.persistence.entity.DiseaseEntity;
import com.itesm.infrastructure.persistence.entity.HospitalEntity;
import com.itesm.infrastructure.persistence.entity.PatientEvaluationEntity;
import com.itesm.infrastructure.persistence.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
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
    HospitalGeoContextService hospitalGeoContextService;

    @Inject
    TranslateDiagnosisAssistantMessagesUseCase translateMessagesUseCase;

    @Inject
    HistoricalCaseRetriever historicalCaseRetriever;

    @Inject
    AssistantSuggestionParser suggestionParser;

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

        HospitalGeoContextDto geoContext = hospitalGeoContextService != null
                ? hospitalGeoContextService.resolve(hospital)
                : fallbackGeoContext(hospital);
        List<Outbreak> outbreaks = new ArrayList<>();
        State state = null;

        if (geoContext.getIncludedMunicipalityIds() != null && !geoContext.getIncludedMunicipalityIds().isEmpty()) {
            outbreaks = outbreakRepository.findActiveByMunicipalityIdsOrStateId(
                    geoContext.getIncludedMunicipalityIds(),
                    geoContext.getStateId());
            if (!outbreaks.isEmpty() && outbreaks.get(0).getState() != null) {
                state = outbreaks.get(0).getState();
            } else {
                state = new State();
                state.setId(geoContext.getStateId());
                state.setName(geoContext.getStateName());
            }
        }

        AssistantMessageDto latestUserMessage = latestUserMessage(request.getMessages());

        DiagnosisAssistantThreadEntity thread = null;
        PatientEvaluationEntity evaluation = null;
        List<AssistantMessageDto> conversationHistory = new ArrayList<>();

        if (request.getEvaluationId() != null) {
            evaluation = loadManagedEvaluation(request.getEvaluationId(), currentUser.getUserId());
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

        List<HistoricalCaseRetriever.HistoricalCase> historical = retrieveHistorical(
                hospitalId,
                evaluation != null ? evaluation.getId() : null,
                evaluation != null ? evaluation.getSymptomsText() : symptomsFromContext(request));

        String systemPrompt = promptBuilder.build(
                state,
                outbreaks,
                request.getPatientContext(),
                geoContext.getRadiusKm(),
                historical);

        List<AssistantChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new AssistantChatMessage("system", systemPrompt));
        for (AssistantMessageDto msg : conversationHistory) {
            chatMessages.add(new AssistantChatMessage(msg.getRole(), msg.getContent()));
        }

        String rawReply = assistantChatGateway.chat(chatMessages);
        AssistantSuggestionParser.ParseResult parsed = suggestionParser.parse(rawReply);
        String displayedReply = parsed.cleanedReply();

        DiagnosisAssistantMessageEntity assistantMessage = null;
        List<AssistantSuggestionDto> persistedSuggestions = List.of();

        if (thread != null) {
            assistantMessage = persistMessage(thread, "assistant", displayedReply, conversationHistory.size() + 1);
            persistedSuggestions = persistSuggestions(assistantMessage, evaluation, parsed.suggestions());
            persistRetrievedCases(assistantMessage, historical);
        } else {
            persistedSuggestions = parsed.suggestions();
        }

        List<OutbreakSummaryDto> outbreakSummaries = outbreaks.stream()
                .filter(o -> o.getDisease() != null)
                .map(o -> new OutbreakSummaryDto(
                        o.getDisease().getName(),
                        o.getScope(),
                        o.getMunicipality() == null ? null : o.getMunicipality().getName(),
                        o.getState() == null ? null : o.getState().getName(),
                        o.getCaseCount(),
                        o.getConfirmationStatus(),
                        o.getStartedAt()))
                .collect(Collectors.toList());

        String stateName = state != null ? state.getName() : null;
        AssistantContextDto contextUsed = new AssistantContextDto(stateName, outbreakSummaries);

        Map<String, String> replyByLanguage = buildReplyByLanguage(displayedReply);

        AssistantResponseDto response = new AssistantResponseDto(displayedReply, replyByLanguage, contextUsed);
        response.setMessageId(assistantMessage != null ? assistantMessage.getId() : null);
        response.setSuggestions(persistedSuggestions);
        return response;
    }

    private Map<String, String> buildReplyByLanguage(String reply) {
        if (translateMessagesUseCase == null) {
            return Map.of();
        }

        return Map.of(
                "en", translateMessagesUseCase.translate(reply, "en"),
                "es", translateMessagesUseCase.translate(reply, "es")
        );
    }

    private List<HistoricalCaseRetriever.HistoricalCase> retrieveHistorical(UUID hospitalId,
                                                                            UUID evaluationId,
                                                                            String symptoms) {
        if (symptoms == null || symptoms.isBlank()) {
            return List.of();
        }
        return historicalCaseRetriever.retrieveSimilar(hospitalId, evaluationId, symptoms);
    }

    private String symptomsFromContext(AssistantRequestDto request) {
        if (request.getPatientContext() == null) {
            return null;
        }
        return request.getPatientContext().getSymptoms();
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

    private List<AssistantSuggestionDto> persistSuggestions(DiagnosisAssistantMessageEntity message,
                                                            PatientEvaluationEntity evaluation,
                                                            List<AssistantSuggestionDto> suggestions) {
        if (suggestions == null || suggestions.isEmpty() || evaluation == null) {
            return List.of();
        }

        List<AssistantSuggestionDto> result = new ArrayList<>(suggestions.size());
        for (AssistantSuggestionDto src : suggestions) {
            DiseaseEntity disease = resolveDiseaseByName(src.getDisplayName());

            DiagnosisAssistantSuggestionEntity entity = new DiagnosisAssistantSuggestionEntity();
            entity.setId(UUID.randomUUID());
            entity.setMessage(message);
            entity.setEvaluation(evaluation);
            entity.setDisease(disease);
            entity.setDisplayName(src.getDisplayName());
            entity.setRankOrder(src.getRankOrder());
            entity.setConfidence(src.getConfidence());
            entity.setRationale(src.getRationale());
            entity.setLocalityRiskLevel(src.getLocalityRiskLevel());
            entity.setWasPrimarySuggestion(src.isPrimary());
            entity.setCreatedAt(LocalDateTime.now());
            entityManager.persist(entity);

            AssistantSuggestionDto dto = new AssistantSuggestionDto();
            dto.setId(entity.getId());
            dto.setMessageId(message.getId());
            dto.setDiseaseId(disease == null ? null : disease.getId());
            dto.setDisplayName(entity.getDisplayName());
            dto.setRankOrder(entity.getRankOrder());
            dto.setConfidence(entity.getConfidence());
            dto.setRationale(entity.getRationale());
            dto.setLocalityRiskLevel(entity.getLocalityRiskLevel());
            dto.setPrimary(entity.isWasPrimarySuggestion());
            result.add(dto);
        }
        return result;
    }

    private void persistRetrievedCases(DiagnosisAssistantMessageEntity message,
                                       List<HistoricalCaseRetriever.HistoricalCase> historical) {
        if (historical == null || historical.isEmpty()) {
            return;
        }
        int rank = 1;
        LocalDateTime now = LocalDateTime.now();
        for (HistoricalCaseRetriever.HistoricalCase hc : historical) {
            DiagnosisAssistantRetrievedCaseEntity entity = new DiagnosisAssistantRetrievedCaseEntity();
            entity.setId(UUID.randomUUID());
            entity.setMessage(message);
            entity.setRetrievedEvaluation(entityManager.getReference(PatientEvaluationEntity.class, hc.evaluationId()));
            entity.setRankOrder(rank++);
            entity.setSimilarityScore(hc.similarityScore());
            entity.setCreatedAt(now);
            entityManager.persist(entity);
        }
    }

    private DiseaseEntity resolveDiseaseByName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return null;
        }
        return entityManager.createQuery("""
                select d
                from DiseaseEntity d
                where lower(d.name) = :name
                """, DiseaseEntity.class)
                .setParameter("name", displayName.trim().toLowerCase(Locale.ROOT))
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    private AssistantMessageDto toAssistantMessageDto(DiagnosisAssistantMessageEntity message) {
        AssistantMessageDto dto = new AssistantMessageDto();
        dto.setId(message.getId());
        dto.setRole(message.getRole());
        dto.setContent(message.getMessageText());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }

    private HospitalGeoContextDto fallbackGeoContext(Hospital hospital) {
        HospitalGeoContextDto geoContext = new HospitalGeoContextDto();
        geoContext.setHospitalId(hospital.getId());
        geoContext.setMunicipalityId(hospital.getMunicipalityId());
        geoContext.setMunicipalityName(hospital.getMunicipalityName());
        geoContext.setStateId(hospital.getStateId());
        geoContext.setStateName(hospital.getStateName());
        geoContext.setLatitude(hospital.getLatitude());
        geoContext.setLongitude(hospital.getLongitude());
        geoContext.setRadiusKm(75);
        geoContext.setIncludedMunicipalityIds(hospital.getMunicipalityId() == null ? List.of() : List.of(hospital.getMunicipalityId()));
        geoContext.setNearbyStates(List.of());
        return geoContext;
    }
}
