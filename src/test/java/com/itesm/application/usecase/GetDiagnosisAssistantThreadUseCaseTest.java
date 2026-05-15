package com.itesm.application.usecase;

import com.itesm.application.dto.AssistantThreadDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.Disease;
import com.itesm.domain.models.Hospital;
import com.itesm.domain.models.Outbreak;
import com.itesm.domain.models.State;
import com.itesm.domain.repository.HospitalRepository;
import com.itesm.domain.repository.OutbreakRepository;
import com.itesm.infrastructure.persistence.entity.DiagnosisAssistantMessageEntity;
import com.itesm.infrastructure.persistence.entity.DiagnosisAssistantSuggestionEntity;
import com.itesm.infrastructure.persistence.entity.DiagnosisAssistantThreadEntity;
import com.itesm.infrastructure.persistence.entity.PatientEvaluationEntity;
import com.itesm.infrastructure.persistence.entity.UserEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

class GetDiagnosisAssistantThreadUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("11000000-0000-0000-0000-000000000001");
    private static final UUID HOSPITAL_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID STATE_ID = UUID.fromString("40000000-0000-0000-0000-000000000019");
    private static final UUID MUNICIPALITY_ID = UUID.fromString("42000000-0000-0000-0000-000000001003");
    private static final UUID EVALUATION_ID = UUID.fromString("82000000-0000-0000-0000-000000000101");
    private static final UUID THREAD_ID = UUID.fromString("83000000-0000-0000-0000-000000000101");

    private GetDiagnosisAssistantThreadUseCase useCase;
    private AuthenticatedUserContext authenticatedUserContext;
    private HospitalRepository hospitalRepository;
    private OutbreakRepository outbreakRepository;
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        useCase = new GetDiagnosisAssistantThreadUseCase();
        authenticatedUserContext = Mockito.mock(AuthenticatedUserContext.class);
        hospitalRepository = Mockito.mock(HospitalRepository.class);
        outbreakRepository = Mockito.mock(OutbreakRepository.class);
        entityManager = Mockito.mock(EntityManager.class);

        useCase.authenticatedUserContext = authenticatedUserContext;
        useCase.hospitalRepository = hospitalRepository;
        useCase.outbreakRepository = outbreakRepository;
        useCase.entityManager = entityManager;

        CurrentUser currentUser = new CurrentUser(
                USER_ID, "ext-id", "doctor@test.local", "Dr. Test",
                HOSPITAL_ID, Set.of("DOCTOR"), Set.of("diagnosis.assist"));
        Mockito.when(authenticatedUserContext.getCurrentUser()).thenReturn(currentUser);
    }

    @Test
    void shouldReturnThreadMessagesAndContextForDoctor() {
        DiagnosisAssistantThreadEntity thread = new DiagnosisAssistantThreadEntity();
        thread.setId(THREAD_ID);
        thread.setCreatedAt(LocalDateTime.now().minusMinutes(2));
        thread.setUpdatedAt(LocalDateTime.now().minusMinutes(1));

        PatientEvaluationEntity evaluation = new PatientEvaluationEntity();
        evaluation.setId(EVALUATION_ID);
        thread.setEvaluation(evaluation);

        UserEntity doctor = new UserEntity();
        doctor.setId(USER_ID);
        thread.setDoctor(doctor);

        DiagnosisAssistantMessageEntity userMessage = message("user", "First question", 1);
        DiagnosisAssistantMessageEntity assistantMessage = message("assistant", "First reply", 2);

        mockThreadQuery(thread);
        mockMessageQuery(List.of(userMessage, assistantMessage));
        mockSuggestionQuery(List.of());

        State state = new State();
        state.setId(STATE_ID);
        state.setName("Nuevo Leon");

        Disease disease = new Disease();
        disease.setId(UUID.randomUUID());
        disease.setName("Measles");

        Outbreak outbreak = new Outbreak();
        outbreak.setId(UUID.randomUUID());
        outbreak.setDisease(disease);
        outbreak.setScope("STATE");
        outbreak.setState(state);
        outbreak.setCaseCount(7);
        outbreak.setConfirmationStatus("CONFIRMED");
        outbreak.setStartedAt(LocalDateTime.now().minusDays(1));

        Hospital hospital = new Hospital();
        hospital.setId(HOSPITAL_ID);
        hospital.setMunicipalityId(MUNICIPALITY_ID);
        hospital.setStateId(STATE_ID);
        hospital.setStateName("Nuevo Leon");

        Mockito.when(hospitalRepository.findHospitalById(HOSPITAL_ID)).thenReturn(Optional.of(hospital));
        Mockito.when(outbreakRepository.findActiveByMunicipalityIdsOrStateId(List.of(MUNICIPALITY_ID), STATE_ID))
                .thenReturn(List.of(outbreak));

        AssistantThreadDto dto = useCase.byEvaluationId(EVALUATION_ID);

        Assertions.assertEquals(THREAD_ID, dto.getId());
        Assertions.assertEquals(EVALUATION_ID, dto.getEvaluationId());
        Assertions.assertEquals(2, dto.getMessages().size());
        Assertions.assertEquals("user", dto.getMessages().get(0).getRole());
        Assertions.assertEquals("First question", dto.getMessages().get(0).getContent());
        Assertions.assertEquals("assistant", dto.getMessages().get(1).getRole());
        Assertions.assertEquals("First reply", dto.getMessages().get(1).getContent());
        Assertions.assertEquals("Nuevo Leon", dto.getContextUsed().getStateName());
        Assertions.assertEquals(1, dto.getContextUsed().getOutbreaks().size());
        Assertions.assertEquals("Measles", dto.getContextUsed().getOutbreaks().get(0).getDiseaseName());
    }

    @Test
    void shouldThrowNotFoundWhenThreadDoesNotExistForDoctor() {
        mockThreadQuery(null);

        Assertions.assertThrows(NotFoundException.class, () -> useCase.byEvaluationId(EVALUATION_ID));
    }

    private DiagnosisAssistantMessageEntity message(String role, String content, int sequenceNo) {
        DiagnosisAssistantMessageEntity entity = new DiagnosisAssistantMessageEntity();
        entity.setId(UUID.randomUUID());
        entity.setRole(role);
        entity.setMessageText(content);
        entity.setSequenceNo(sequenceNo);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }

    @SuppressWarnings("unchecked")
    private void mockThreadQuery(DiagnosisAssistantThreadEntity thread) {
        TypedQuery<DiagnosisAssistantThreadEntity> query = Mockito.mock(TypedQuery.class);
        Mockito.when(entityManager.createQuery(Mockito.anyString(), Mockito.eq(DiagnosisAssistantThreadEntity.class)))
                .thenReturn(query);
        Mockito.when(query.setParameter(Mockito.eq("evaluationId"), Mockito.any()))
                .thenReturn(query);
        Mockito.when(query.setParameter(Mockito.eq("doctorId"), Mockito.any()))
                .thenReturn(query);
        Mockito.when(query.getResultStream())
                .thenReturn(thread == null ? Stream.empty() : Stream.of(thread));
    }

    @SuppressWarnings("unchecked")
    private void mockMessageQuery(List<DiagnosisAssistantMessageEntity> messages) {
        TypedQuery<DiagnosisAssistantMessageEntity> query = Mockito.mock(TypedQuery.class);
        Mockito.when(entityManager.createQuery(Mockito.contains("from DiagnosisAssistantMessageEntity"), Mockito.eq(DiagnosisAssistantMessageEntity.class)))
                .thenReturn(query);
        Mockito.when(query.setParameter(Mockito.eq("threadId"), Mockito.any()))
                .thenReturn(query);
        Mockito.when(query.getResultList()).thenReturn(messages);
        Mockito.when(query.getResultStream()).thenReturn(messages.stream());
    }

    @SuppressWarnings("unchecked")
    private void mockSuggestionQuery(List<DiagnosisAssistantSuggestionEntity> rows) {
        TypedQuery<DiagnosisAssistantSuggestionEntity> query = Mockito.mock(TypedQuery.class);
        Mockito.when(entityManager.createQuery(Mockito.contains("from DiagnosisAssistantSuggestionEntity"), Mockito.eq(DiagnosisAssistantSuggestionEntity.class)))
                .thenReturn(query);
        Mockito.when(query.setParameter(Mockito.eq("messageIds"), Mockito.any()))
                .thenReturn(query);
        Mockito.when(query.getResultList()).thenReturn(rows);
    }
}
