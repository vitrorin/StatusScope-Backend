package com.itesm.interfaces.rest;

import com.itesm.application.port.out.AssistantChatGateway;
import com.itesm.domain.models.User;
import com.itesm.domain.models.UserStatus;
import com.itesm.domain.repository.RoleRepository;
import com.itesm.domain.repository.UserRepository;
import com.itesm.infrastructure.persistence.entity.DiseaseEntity;
import com.itesm.infrastructure.persistence.entity.EventEntity;
import com.itesm.infrastructure.persistence.entity.HospitalEntity;
import com.itesm.infrastructure.persistence.entity.MunicipalityEntity;
import com.itesm.infrastructure.persistence.entity.OutbreakEntity;
import com.itesm.infrastructure.persistence.entity.PatientEntity;
import com.itesm.infrastructure.persistence.entity.PatientEvaluationEntity;
import com.itesm.infrastructure.persistence.entity.UserEntity;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;
import java.util.UUID;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class DiagnosisAssistantResourceTest {

    private static final String DOCTOR_EMAIL = "diagtest-doctor@statusscope.local";
    private static final String ADMIN_EMAIL = "diagtest-admin@statusscope.local";
    private static final UUID HOSPITAL_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID MUNICIPALITY_ID = UUID.fromString("42000000-0000-0000-0000-000000001003");
    private static final UUID DISEASE_ID = UUID.fromString("60000000-0000-0000-0000-000000000004");
    private static final UUID OUTBREAK_ID = UUID.fromString("81000000-0000-0000-0000-000000000001");
    private static final UUID PATIENT_ID = UUID.fromString("84000000-0000-0000-0000-000000000001");
    private static final UUID EVALUATION_ID = UUID.fromString("84000000-0000-0000-0000-000000000101");
    private static final UUID EVENT_ID = UUID.fromString("84000000-0000-0000-0000-000000000201");

    @InjectMock
    AssistantChatGateway assistantChatGateway;

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    EntityManager entityManager;

    @BeforeEach
    @Transactional
    void seedTestUsers() {
        Mockito.when(assistantChatGateway.chat(Mockito.anyList()))
                .thenReturn("Based on the active measles outbreak, consider Koplik spots.");

        User doctor = userRepository.findByEmail(DOCTOR_EMAIL).orElseGet(() -> {
            var doctorRole = roleRepository.findByCode("DOCTOR").orElseThrow();
            User newDoctor = new User();
            newDoctor.setId(UUID.randomUUID());
            newDoctor.setFullName("Test Doctor");
            newDoctor.setEmail(DOCTOR_EMAIL);
            newDoctor.setExternalAuthId("test-doctor-ext");
            newDoctor.setStatus(UserStatus.ACTIVE);
            newDoctor.setHospitalId(HOSPITAL_ID);
            newDoctor.setRoles(Set.of(doctorRole));
            return userRepository.create(newDoctor);
        });

        if (userRepository.findByEmail(ADMIN_EMAIL).isEmpty()) {
            var adminRole = roleRepository.findByCode("HOSPITAL_ADMIN").orElseThrow();
            User admin = new User();
            admin.setId(UUID.randomUUID());
            admin.setFullName("Test Admin");
            admin.setEmail(ADMIN_EMAIL);
            admin.setExternalAuthId("test-admin-ext");
            admin.setStatus(UserStatus.ACTIVE);
            admin.setHospitalId(HOSPITAL_ID);
            admin.setRoles(Set.of(adminRole));
            userRepository.create(admin);
        }

        if (entityManager.find(OutbreakEntity.class, OUTBREAK_ID) == null) {
            DiseaseEntity disease = entityManager.getReference(DiseaseEntity.class, DISEASE_ID);
            MunicipalityEntity municipality = entityManager.getReference(MunicipalityEntity.class, MUNICIPALITY_ID);

            OutbreakEntity outbreak = new OutbreakEntity();
            outbreak.setId(OUTBREAK_ID);
            outbreak.setDisease(disease);
            outbreak.setScope("MUNICIPALITY");
            outbreak.setMunicipality(municipality);
            outbreak.setCaseCount(12);
            outbreak.setConfirmationStatus("CONFIRMED");
            outbreak.setStatus("ACTIVE");
            outbreak.setStartedAt(LocalDateTime.now().minusDays(3));
            outbreak.setCreatedAt(LocalDateTime.now());
            outbreak.setUpdatedAt(LocalDateTime.now());
            entityManager.persist(outbreak);
        }

        seedEvaluation(doctor);
    }

    @Test
    void shouldReturn401WhenNoToken() {
        given()
                .contentType(ContentType.JSON)
                .body(buildRequestBody("What disease causes Koplik spots?"))
                .when()
                .post("/diagnosis/assistant/messages")
                .then()
                .statusCode(401);
    }

    @Test
    void shouldReturn403WhenCallerIsHospitalAdmin() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .body(buildRequestBody("What disease causes Koplik spots?"))
                .when()
                .post("/diagnosis/assistant/messages")
                .then()
                .statusCode(403);
    }

    @Test
    void shouldReturn200WithReplyWhenCallerIsDoctor() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .body(buildRequestBody("Patient presents with fever, rash and spots on buccal mucosa"))
                .when()
                .post("/diagnosis/assistant/messages")
                .then()
                .statusCode(200)
                .body("reply", notNullValue());
    }

    @Test
    void shouldReturnContextUsedInResponse() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .body(buildRequestBody("Patient has fever"))
                .when()
                .post("/diagnosis/assistant/messages")
                .then()
                .statusCode(200)
                .body("contextUsed", notNullValue())
                .body("contextUsed.stateName", notNullValue())
                .body("contextUsed.outbreaks.findAll { it.diseaseName == 'COVID-19' && it.confirmationStatus == 'CONFIRMED' }.caseCount",
                        hasItem(12));
    }

    @Test
    void shouldReturn400WhenMessagesAreEmpty() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .body("""
                        {
                          "messages": []
                        }
                        """)
                .when()
                .post("/diagnosis/assistant/messages")
                .then()
                .statusCode(400);
    }

    @Test
    void shouldPersistEvaluationThreadAndReturnConversationHistory() {
        Mockito.when(assistantChatGateway.chat(Mockito.anyList()))
                .thenReturn("Initial assistant reply.", "Follow-up assistant reply.");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .body(buildEvaluationRequestBody(EVALUATION_ID, "First question"))
                .when()
                .post("/diagnosis/assistant/messages")
                .then()
                .statusCode(200)
                .body("reply", equalTo("Initial assistant reply."));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .body(buildEvaluationRequestBody(EVALUATION_ID, "Second question"))
                .when()
                .post("/diagnosis/assistant/messages")
                .then()
                .statusCode(200)
                .body("reply", equalTo("Follow-up assistant reply."));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .when()
                .get("/diagnosis/assistant/evaluations/{evaluationId}/thread", EVALUATION_ID)
                .then()
                .statusCode(200)
                .body("evaluationId", equalTo(EVALUATION_ID.toString()))
                .body("messages", hasSize(4))
                .body("messages[0].role", equalTo("user"))
                .body("messages[0].content", equalTo("First question"))
                .body("messages[1].role", equalTo("assistant"))
                .body("messages[1].content", equalTo("Initial assistant reply."))
                .body("messages[2].role", equalTo("user"))
                .body("messages[2].content", equalTo("Second question"))
                .body("messages[3].role", equalTo("assistant"))
                .body("messages[3].content", equalTo("Follow-up assistant reply."))
                .body("contextUsed.stateName", notNullValue())
                .body("contextUsed.outbreaks.caseCount", hasItem(12));
    }

    @Test
    void shouldReturn404WhenThreadDoesNotExistForEvaluation() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .when()
                .get("/diagnosis/assistant/evaluations/{evaluationId}/thread",
                        UUID.fromString("84000000-0000-0000-0000-000000000999"))
                .then()
                .statusCode(404);
    }

    @Test
    void translationsShouldReturn401WhenNoToken() {
        given()
                .contentType(ContentType.JSON)
                .body(buildTranslationBody())
                .when()
                .post("/diagnosis/assistant/translations")
                .then()
                .statusCode(401);
    }

    @Test
    void translationsShouldReturn403WhenCallerIsHospitalAdmin() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .body(buildTranslationBody())
                .when()
                .post("/diagnosis/assistant/translations")
                .then()
                .statusCode(403);
    }

    @Test
    void translationsShouldReturn200AndTranslateMessages() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .body(buildTranslationBody())
                .when()
                .post("/diagnosis/assistant/translations")
                .then()
                .statusCode(200)
                .body("translations", notNullValue())
                .body("translations.size()", equalTo(1))
                .body("translations[0].clientId", equalTo("msg-001"));
    }

    private String buildRequestBody(String content) {
        return """
                {
                  "messages": [
                    { "role": "user", "content": "%s" }
                  ]
                }
                """.formatted(content);
    }

    private String buildEvaluationRequestBody(UUID evaluationId, String content) {
        return """
                {
                  "evaluationId": "%s",
                  "messages": [
                    { "role": "user", "content": "%s" }
                  ]
                }
                """.formatted(evaluationId, content);
    }

    private String buildTranslationBody() {
        return """
                {
                  "targetLanguage": "es",
                  "messages": [
                    { "clientId": "msg-001", "role": "assistant", "content": "Patient may have dengue." }
                  ]
                }
                """;
    }

    private void seedEvaluation(User doctor) {
        HospitalEntity hospital = entityManager.getReference(HospitalEntity.class, HOSPITAL_ID);
        DiseaseEntity disease = entityManager.getReference(DiseaseEntity.class, DISEASE_ID);
        UserEntity doctorEntity = entityManager.getReference(UserEntity.class, doctor.getId());

        PatientEntity patient = entityManager.find(PatientEntity.class, PATIENT_ID);
        boolean newPatient = false;
        if (patient == null) {
            patient = new PatientEntity();
            patient.setId(PATIENT_ID);
            patient.setCreatedAt(LocalDateTime.now());
            newPatient = true;
        }
        patient.setHospital(hospital);
        patient.setFullName("Assistant Test Patient");
        patient.setSex("female");
        patient.setBirthDate(LocalDate.now().minusYears(8));
        patient.setWeightKg(new BigDecimal("27.50"));
        patient.setHeightCm(new BigDecimal("128.00"));
        patient.setPostalCode("64000");
        patient.setUpdatedAt(LocalDateTime.now());
        if (newPatient) {
            entityManager.persist(patient);
        }

        EventEntity event = entityManager.find(EventEntity.class, EVENT_ID);
        if (event == null) {
            event = new EventEntity();
            event.setId(EVENT_ID);
            event.setPatient(patient);
            event.setDisease(disease);
            event.setPrimaryDoctor(doctorEntity);
            event.setStatus("ACTIVE");
            event.setStartedAt(LocalDateTime.now().minusHours(4));
            event.setCreatedAt(LocalDateTime.now());
            event.setUpdatedAt(LocalDateTime.now());
            entityManager.persist(event);
        }

        PatientEvaluationEntity evaluation = entityManager.find(PatientEvaluationEntity.class, EVALUATION_ID);
        boolean newEvaluation = false;
        if (evaluation == null) {
            evaluation = new PatientEvaluationEntity();
            evaluation.setId(EVALUATION_ID);
            evaluation.setCreatedAt(LocalDateTime.now());
            newEvaluation = true;
        }
        evaluation.setPatient(patient);
        evaluation.setDoctor(doctorEntity);
        evaluation.setEvent(event);
        evaluation.setStatus("IN_PROGRESS");
        evaluation.setSymptomsText("Fever, rash, oral spots.");
        evaluation.setClinicalNotes("Diagnosis assistant resource test.");
        evaluation.setUpdatedAt(LocalDateTime.now());
        if (newEvaluation) {
            entityManager.persist(evaluation);
        }
    }
}
