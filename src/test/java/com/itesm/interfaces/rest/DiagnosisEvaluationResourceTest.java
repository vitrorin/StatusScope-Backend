package com.itesm.interfaces.rest;

import com.itesm.domain.models.User;
import com.itesm.domain.models.UserStatus;
import com.itesm.domain.repository.RoleRepository;
import com.itesm.domain.repository.UserRepository;
import com.itesm.infrastructure.persistence.entity.DiseaseEntity;
import com.itesm.infrastructure.persistence.entity.EvaluationDifferentialDiagnosisEntity;
import com.itesm.infrastructure.persistence.entity.EvaluationRecommendedTestEntity;
import com.itesm.infrastructure.persistence.entity.EventEntity;
import com.itesm.infrastructure.persistence.entity.HospitalEntity;
import com.itesm.infrastructure.persistence.entity.PatientEntity;
import com.itesm.infrastructure.persistence.entity.PatientEvaluationEntity;
import com.itesm.infrastructure.persistence.entity.PatientEvaluationFileEntity;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class DiagnosisEvaluationResourceTest {

    private static final String DOCTOR_EMAIL = "evaluation-doctor@statusscope.local";
    private static final String ADMIN_EMAIL = "evaluation-admin@statusscope.local";
    private static final UUID HOSPITAL_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID DISEASE_ID = UUID.fromString("60000000-0000-0000-0000-000000000004");
    private static final UUID PATIENT_ID = UUID.fromString("82000000-0000-0000-0000-000000000001");
    private static final UUID EVALUATION_ID = UUID.fromString("82000000-0000-0000-0000-000000000101");
    private static final UUID EVENT_ID = UUID.fromString("82000000-0000-0000-0000-000000000201");
    private static final UUID TEST_ID = UUID.fromString("82000000-0000-0000-0000-000000000301");
    private static final UUID DIFFERENTIAL_ID = UUID.fromString("82000000-0000-0000-0000-000000000401");
    private static final UUID FILE_ID = UUID.fromString("82000000-0000-0000-0000-000000000501");

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    EntityManager entityManager;

    @BeforeEach
    @Transactional
    void seedEvaluation() {
        User doctor = userRepository.findByEmail(DOCTOR_EMAIL).orElseGet(() -> {
            var doctorRole = roleRepository.findByCode("DOCTOR").orElseThrow();
            User newDoctor = new User();
            newDoctor.setId(UUID.randomUUID());
            newDoctor.setFullName("Evaluation Doctor");
            newDoctor.setEmail(DOCTOR_EMAIL);
            newDoctor.setExternalAuthId("evaluation-doctor-ext");
            newDoctor.setStatus(UserStatus.ACTIVE);
            newDoctor.setHospitalId(HOSPITAL_ID);
            newDoctor.setRoles(Set.of(doctorRole));
            return userRepository.create(newDoctor);
        });

        if (userRepository.findByEmail(ADMIN_EMAIL).isEmpty()) {
            var adminRole = roleRepository.findByCode("HOSPITAL_ADMIN").orElseThrow();
            User admin = new User();
            admin.setId(UUID.randomUUID());
            admin.setFullName("Evaluation Admin");
            admin.setEmail(ADMIN_EMAIL);
            admin.setExternalAuthId("evaluation-admin-ext");
            admin.setStatus(UserStatus.ACTIVE);
            admin.setHospitalId(HOSPITAL_ID);
            admin.setRoles(Set.of(adminRole));
            userRepository.create(admin);
        }

        HospitalEntity hospital = entityManager.getReference(HospitalEntity.class, HOSPITAL_ID);
        DiseaseEntity disease = entityManager.getReference(DiseaseEntity.class, DISEASE_ID);

        PatientEntity patient = entityManager.find(PatientEntity.class, PATIENT_ID);
        boolean newPatient = false;
        if (patient == null) {
            patient = new PatientEntity();
            patient.setId(PATIENT_ID);
            patient.setCreatedAt(LocalDateTime.now());
            newPatient = true;
        }
        patient.setHospital(hospital);
        patient.setFullName("Lucia Herrera");
        patient.setSex("female");
        patient.setBirthDate(LocalDate.now().minusYears(9));
        patient.setWeightKg(new BigDecimal("29.10"));
        patient.setHeightCm(new BigDecimal("132.00"));
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
            event.setPrimaryDoctor(entityManager.getReference(
                    com.itesm.infrastructure.persistence.entity.UserEntity.class, doctor.getId()));
            event.setStatus("ACTIVE");
            event.setStartedAt(LocalDateTime.now().minusHours(6));
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
        evaluation.setDoctor(entityManager.getReference(
                com.itesm.infrastructure.persistence.entity.UserEntity.class, doctor.getId()));
        evaluation.setEvent(event);
        evaluation.setStatus("IN_PROGRESS");
        evaluation.setSymptomsText("High fever, cough, fatigue, and loss of smell.");
        evaluation.setClinicalNotes("Resource test evaluation.");
        evaluation.setUpdatedAt(LocalDateTime.now());
        if (newEvaluation) {
            entityManager.persist(evaluation);
        }

        if (entityManager.find(EvaluationRecommendedTestEntity.class, TEST_ID) == null) {
            EvaluationRecommendedTestEntity test = new EvaluationRecommendedTestEntity();
            test.setId(TEST_ID);
            test.setEvaluation(evaluation);
            test.setTestName("SARS-CoV-2 PCR");
            test.setReason("Symptoms overlap with the active respiratory profile.");
            test.setSource("AI");
            test.setSortOrder(0);
            test.setCreatedAt(LocalDateTime.now());
            entityManager.persist(test);
        }

        if (entityManager.find(EvaluationDifferentialDiagnosisEntity.class, DIFFERENTIAL_ID) == null) {
            EvaluationDifferentialDiagnosisEntity differential = new EvaluationDifferentialDiagnosisEntity();
            differential.setId(DIFFERENTIAL_ID);
            differential.setEvaluation(evaluation);
            differential.setDisease(disease);
            differential.setDisplayName("COVID-19");
            differential.setConfidence(new BigDecimal("84.50"));
            differential.setRationale("Matches the respiratory symptom cluster.");
            differential.setRankOrder(0);
            differential.setLocalityRiskLevel("HIGH");
            differential.setCreatedAt(LocalDateTime.now());
            entityManager.persist(differential);
        }

        if (entityManager.find(PatientEvaluationFileEntity.class, FILE_ID) == null) {
            PatientEvaluationFileEntity file = new PatientEvaluationFileEntity();
            file.setId(FILE_ID);
            file.setEvaluation(evaluation);
            file.setFileName("lab-panel.pdf");
            file.setMimeType("application/pdf");
            file.setStorageKey("test/lab-panel.pdf");
            file.setFileSizeBytes(12345L);
            file.setDocumentType("LAB_RESULT");
            file.setUploadedAt(LocalDateTime.now());
            entityManager.persist(file);
        }
    }

    @Test
    void shouldReturn401WhenNoToken() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/diagnosis/evaluations/current")
                .then()
                .statusCode(401);
    }

    @Test
    void shouldReturn403WhenCallerCannotUseDiagnosisAssistant() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .get("/diagnosis/evaluations/current")
                .then()
                .statusCode(403);
    }

    @Test
    void shouldCreateEvaluationWithoutPriorFetch() {
        String payload = """
                {
                  "patientFullName": "New Patient",
                  "birthDate": "2014-05-20",
                  "sex": "female",
                  "symptomsText": "Fever and sore throat."
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .body(payload)
                .when()
                .post("/diagnosis/evaluations")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("status", equalTo("IN_PROGRESS"))
                .body("patient.fullName", equalTo("New Patient"))
                .body("patient.birthDate", equalTo("2014-05-20"))
                .body("symptomsText", equalTo("Fever and sore throat."));
    }

    @Test
    void shouldReturnCurrentEvaluationWithDatabaseBackedDetails() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .when()
                .get("/diagnosis/evaluations/current")
                .then()
                .statusCode(200)
                .body("id", equalTo(EVALUATION_ID.toString()))
                .body("patient.fullName", equalTo("Lucia Herrera"))
                .body("patient.ageYears", equalTo(9))
                .body("event.diseaseName", equalTo("COVID-19"))
                .body("differentialDiagnoses[0].displayName", equalTo("COVID-19"))
                .body("differentialDiagnoses[0].localityRiskLevel", equalTo("HIGH"))
                .body("recommendedTests[0].testName", equalTo("SARS-CoV-2 PCR"))
                .body("files[0].fileName", equalTo("lab-panel.pdf"));
    }

    @Test
    void shouldListNormalizedDiseasesForDiagnosisFeedback() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .queryParam("query", "covid")
                .when()
                .get("/diagnosis/diseases")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0))
                .body("name", hasItem("COVID-19"));
    }

    @Test
    void shouldReturnEvaluationById() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .when()
                .get("/diagnosis/evaluations/{id}", EVALUATION_ID)
                .then()
                .statusCode(200)
                .body("id", equalTo(EVALUATION_ID.toString()))
                .body("createdAt", notNullValue());
    }

    @Test
    void shouldUpdateEvaluationFields() {
        String payload = """
                {
                  "patientFullName": "Camila Herrera",
                  "birthDate": "2015-04-20",
                  "sex": "female",
                  "symptomsText": "Persistent fever and body ache."
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .body(payload)
                .when()
                .put("/diagnosis/evaluations/{id}", EVALUATION_ID)
                .then()
                .statusCode(200)
                .body("patient.fullName", equalTo("Camila Herrera"))
                .body("patient.birthDate", equalTo("2015-04-20"))
                .body("symptomsText", equalTo("Persistent fever and body ache."));
    }

    @Test
    void shouldUpdateEvaluationStatus() {
        String payload = """
                {
                  "status": "CONFIRMED"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .body(payload)
                .when()
                .post("/diagnosis/evaluations/{id}/status", EVALUATION_ID)
                .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMED"))
                .body("finalizedAt", notNullValue());
    }

    @Test
    void shouldRecordAssistantFeedbackWithNormalizedDisease() {
        String payload = """
                {
                  "finalDecisionSource": "DOCTOR_ONLY",
                  "finalDiseaseId": "%s",
                  "doctorFeedbackNotes": "Confirmed after lab review."
                }
                """.formatted(DISEASE_ID);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .body(payload)
                .when()
                .post("/diagnosis/evaluations/{id}/assistant-feedback", EVALUATION_ID)
                .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMED"))
                .body("finalDecisionSource", equalTo("DOCTOR_ONLY"))
                .body("finalDiseaseId", equalTo(DISEASE_ID.toString()))
                .body("finalDiseaseName", equalTo("COVID-19"))
                .body("finalDiagnosisLabel", equalTo("COVID-19"))
                .body("doctorFeedbackNotes", equalTo("Confirmed after lab review."));
    }

    @Test
    void shouldUploadEvaluationFileMetadata() {
        String payload = """
                {
                  "fileName": "updated-lab-panel.pdf",
                  "mimeType": "application/pdf",
                  "fileSizeBytes": 2048,
                  "documentType": "LAB_RESULT",
                  "contentBase64": "dGVzdA=="
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .body(payload)
                .when()
                .post("/diagnosis/evaluations/{id}/files", EVALUATION_ID)
                .then()
                .statusCode(200)
                .body("files[0].fileName", equalTo("updated-lab-panel.pdf"));
    }
}
