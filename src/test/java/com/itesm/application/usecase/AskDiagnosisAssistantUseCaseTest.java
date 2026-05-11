package com.itesm.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itesm.application.dto.AssistantContextDto;
import com.itesm.application.dto.AssistantMessageDto;
import com.itesm.application.dto.AssistantRequestDto;
import com.itesm.application.dto.AssistantResponseDto;
import com.itesm.application.dto.PatientContextDto;
import com.itesm.application.port.out.AssistantChatGateway;
import com.itesm.application.port.out.AssistantChatMessage;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.Disease;
import com.itesm.domain.models.Hospital;
import com.itesm.domain.models.Municipality;
import com.itesm.domain.models.Outbreak;
import com.itesm.domain.models.State;
import com.itesm.domain.repository.HospitalRepository;
import com.itesm.domain.repository.OutbreakRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

class AskDiagnosisAssistantUseCaseTest {

    private AskDiagnosisAssistantUseCase useCase;
    private AuthenticatedUserContext authenticatedUserContext;
    private HospitalRepository hospitalRepository;
    private OutbreakRepository outbreakRepository;
    private AssistantChatGateway assistantChatGateway;

    private final UUID hospitalId = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private final UUID stateId = UUID.fromString("40000000-0000-0000-0000-000000000019");
    private final UUID municipalityId = UUID.fromString("42000000-0000-0000-0000-000000001003");

    @BeforeEach
    void setUp() {
        authenticatedUserContext = Mockito.mock(AuthenticatedUserContext.class);
        hospitalRepository = Mockito.mock(HospitalRepository.class);
        outbreakRepository = Mockito.mock(OutbreakRepository.class);
        assistantChatGateway = Mockito.mock(AssistantChatGateway.class);

        useCase = new AskDiagnosisAssistantUseCase();
        useCase.authenticatedUserContext = authenticatedUserContext;
        useCase.hospitalRepository = hospitalRepository;
        useCase.outbreakRepository = outbreakRepository;
        useCase.assistantChatGateway = assistantChatGateway;
        useCase.promptBuilder = new AssistantPromptBuilder();
        useCase.historicalCaseRetriever = Mockito.mock(HistoricalCaseRetriever.class);
        Mockito.when(useCase.historicalCaseRetriever.retrieveSimilar(
                Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(List.of());

        AssistantSuggestionParser parser = new AssistantSuggestionParser();
        parser.objectMapper = new ObjectMapper();
        useCase.suggestionParser = parser;

        CurrentUser currentUser = new CurrentUser(
                UUID.randomUUID(), "ext-id", "doctor@test.local", "Dr. Test",
                hospitalId, Set.of("DOCTOR"), Set.of("diagnosis.assist"));
        Mockito.when(authenticatedUserContext.getCurrentUser()).thenReturn(currentUser);

        Hospital hospital = new Hospital();
        hospital.setId(hospitalId);
        hospital.setMunicipalityId(municipalityId);
        hospital.setStateId(stateId);
        hospital.setStateName("Nuevo Leon");
        Mockito.when(hospitalRepository.findHospitalById(hospitalId)).thenReturn(Optional.of(hospital));

        Mockito.when(assistantChatGateway.chat(Mockito.anyList())).thenReturn("Consider measles given local outbreak.");
    }

    @Test
    void shouldQueryOutbreaksWithDoctorMunicipalityScope() {
        Mockito.when(outbreakRepository.findActiveByMunicipalityIdsOrStateId(List.of(municipalityId), stateId)).thenReturn(List.of());

        AssistantRequestDto request = buildRequest("Patient has fever and rash");
        useCase.execute(request);

        Mockito.verify(outbreakRepository).findActiveByMunicipalityIdsOrStateId(List.of(municipalityId), stateId);
    }

    @Test
    void shouldPrependSystemMessageBeforeDoctorTurns() {
        Mockito.when(outbreakRepository.findActiveByMunicipalityIdsOrStateId(List.of(municipalityId), stateId)).thenReturn(List.of());

        AssistantRequestDto request = buildRequest("Patient has fever");
        useCase.execute(request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AssistantChatMessage>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(assistantChatGateway).chat(captor.capture());

        List<AssistantChatMessage> messages = captor.getValue();
        Assertions.assertEquals("system", messages.get(0).getRole());
        Assertions.assertEquals("user", messages.get(1).getRole());
        Assertions.assertEquals("Patient has fever", messages.get(1).getContent());
    }

    @Test
    void shouldForwardPriorTurnsUnchanged() {
        Mockito.when(outbreakRepository.findActiveByMunicipalityIdsOrStateId(List.of(municipalityId), stateId)).thenReturn(List.of());

        AssistantMessageDto userMsg = new AssistantMessageDto();
        userMsg.setRole("user");
        userMsg.setContent("First question");
        AssistantMessageDto assistantMsg = new AssistantMessageDto();
        assistantMsg.setRole("assistant");
        assistantMsg.setContent("Prior reply");
        AssistantMessageDto newUserMsg = new AssistantMessageDto();
        newUserMsg.setRole("user");
        newUserMsg.setContent("New question");

        AssistantRequestDto request = new AssistantRequestDto();
        request.setMessages(List.of(userMsg, assistantMsg, newUserMsg));

        useCase.execute(request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AssistantChatMessage>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(assistantChatGateway).chat(captor.capture());
        List<AssistantChatMessage> sent = captor.getValue();

        // 1 system + 3 original turns
        Assertions.assertEquals(4, sent.size());
        Assertions.assertEquals("First question", sent.get(1).getContent());
        Assertions.assertEquals("Prior reply", sent.get(2).getContent());
        Assertions.assertEquals("New question", sent.get(3).getContent());
    }

    @Test
    void shouldIncludeOutbreaksInContextUsed() {
        Disease disease = new Disease();
        disease.setId(UUID.randomUUID());
        disease.setName("Measles");
        disease.setSymptoms("Fever, rash, Koplik spots");

        State state = new State();
        state.setId(stateId);
        state.setName("Nuevo Leon");

        Outbreak outbreak = new Outbreak();
        outbreak.setId(UUID.randomUUID());
        outbreak.setDisease(disease);
        outbreak.setScope("STATE");
        outbreak.setState(state);
        outbreak.setCaseCount(12);
        outbreak.setConfirmationStatus("SUSPECTED");
        outbreak.setStartedAt(LocalDateTime.now().minusDays(3));
        outbreak.setStatus("ACTIVE");

        Mockito.when(outbreakRepository.findActiveByMunicipalityIdsOrStateId(List.of(municipalityId), stateId)).thenReturn(List.of(outbreak));

        AssistantResponseDto response = useCase.execute(buildRequest("Patient has fever and spots"));

        AssistantContextDto ctx = response.getContextUsed();
        Assertions.assertEquals(1, ctx.getOutbreaks().size());
        Assertions.assertEquals("Measles", ctx.getOutbreaks().get(0).getDiseaseName());
        Assertions.assertEquals("STATE", ctx.getOutbreaks().get(0).getScope());
        Assertions.assertEquals("SUSPECTED", ctx.getOutbreaks().get(0).getConfirmationStatus());
        Assertions.assertEquals(12, ctx.getOutbreaks().get(0).getCaseCount());
    }

    @Test
    void shouldThrowWhenLatestMessageIsNotFromUser() {
        Mockito.when(outbreakRepository.findActiveByMunicipalityIdsOrStateId(List.of(municipalityId), stateId)).thenReturn(List.of());

        AssistantMessageDto userMsg = new AssistantMessageDto();
        userMsg.setRole("user");
        userMsg.setContent("Initial question");

        AssistantMessageDto assistantMsg = new AssistantMessageDto();
        assistantMsg.setRole("assistant");
        assistantMsg.setContent("Last reply");

        AssistantRequestDto request = new AssistantRequestDto();
        request.setMessages(List.of(userMsg, assistantMsg));

        Assertions.assertThrows(IllegalArgumentException.class, () -> useCase.execute(request));
        Mockito.verifyNoInteractions(assistantChatGateway);
    }

    @Test
    void shouldThrowNotFoundWhenHospitalDoesNotExist() {
        Mockito.when(hospitalRepository.findHospitalById(hospitalId)).thenReturn(Optional.empty());

        AssistantRequestDto request = buildRequest("Some question");

        Assertions.assertThrows(NotFoundException.class, () -> useCase.execute(request));
        Mockito.verifyNoInteractions(outbreakRepository);
        Mockito.verifyNoInteractions(assistantChatGateway);
    }

    @Test
    void shouldThrowNotFoundWhenDoctorHasNoHospital() {
        CurrentUser noHospitalUser = new CurrentUser(
                UUID.randomUUID(), "ext", "nohospital@test.local", "No Hospital",
                null, Set.of("DOCTOR"), Set.of("diagnosis.assist"));
        Mockito.when(authenticatedUserContext.getCurrentUser()).thenReturn(noHospitalUser);

        AssistantRequestDto request = buildRequest("Some question");
        Assertions.assertThrows(NotFoundException.class, () -> useCase.execute(request));
    }

    private AssistantRequestDto buildRequest(String content) {
        AssistantMessageDto msg = new AssistantMessageDto();
        msg.setRole("user");
        msg.setContent(content);

        AssistantRequestDto request = new AssistantRequestDto();
        request.setMessages(List.of(msg));
        return request;
    }
}

