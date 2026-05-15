package com.itesm.application.usecase;

import com.itesm.application.dto.AssistantContextDto;
import com.itesm.application.dto.AssistantMessageDto;
import com.itesm.application.dto.AssistantRequestDto;
import com.itesm.application.dto.AssistantResponseDto;
import com.itesm.application.dto.HospitalGeoContextDto;
import com.itesm.application.dto.OutbreakSummaryDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.Hospital;
import com.itesm.domain.models.Outbreak;
import com.itesm.domain.models.State;
import com.itesm.domain.repository.HospitalRepository;
import com.itesm.domain.repository.OutbreakRepository;
import com.itesm.infrastructure.llm.LlmChatClient;
import com.itesm.infrastructure.openai.dto.ChatMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
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
    LlmChatClient llmChatClient;

    @Inject
    AssistantPromptBuilder promptBuilder;

    @Inject
    HospitalGeoContextService hospitalGeoContextService;

    @Inject
    TranslateDiagnosisAssistantMessagesUseCase translateMessagesUseCase;

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

        String systemPrompt = promptBuilder.build(state, outbreaks, request.getPatientContext(), geoContext.getRadiusKm());

        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatMessage("system", systemPrompt));
        for (AssistantMessageDto msg : request.getMessages()) {
            chatMessages.add(new ChatMessage(msg.getRole(), msg.getContent()));
        }

        String reply = llmChatClient.chat(chatMessages);

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

        Map<String, String> replyByLanguage = buildReplyByLanguage(reply);

        return new AssistantResponseDto(reply, replyByLanguage, contextUsed);
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
