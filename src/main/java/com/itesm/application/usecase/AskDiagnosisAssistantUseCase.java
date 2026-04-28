package com.itesm.application.usecase;

import com.itesm.application.dto.AssistantContextDto;
import com.itesm.application.dto.AssistantMessageDto;
import com.itesm.application.dto.AssistantRequestDto;
import com.itesm.application.dto.AssistantResponseDto;
import com.itesm.application.dto.OutbreakSummaryDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.Hospital;
import com.itesm.domain.models.Outbreak;
import com.itesm.domain.models.Region;
import com.itesm.domain.repository.HospitalRepository;
import com.itesm.domain.repository.OutbreakRepository;
import com.itesm.infrastructure.llm.LlmChatClient;
import com.itesm.infrastructure.openai.dto.ChatMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
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
                        o.getCaseCount(),
                        o.getStartedAt()))
                .collect(Collectors.toList());

        String regionName = region != null ? region.getName() : null;
        AssistantContextDto contextUsed = new AssistantContextDto(regionName, outbreakSummaries);

        return new AssistantResponseDto(reply, contextUsed);
    }
}
