package com.itesm.interfaces.rest;

import com.itesm.application.port.out.AssistantChatGateway;
import com.itesm.domain.models.Hospital;
import com.itesm.domain.models.User;
import com.itesm.domain.models.UserStatus;
import com.itesm.domain.repository.RoleRepository;
import com.itesm.domain.repository.UserRepository;
import com.itesm.infrastructure.persistence.entity.DiseaseEntity;
import com.itesm.infrastructure.persistence.entity.OutbreakEntity;
import com.itesm.infrastructure.persistence.entity.RegionEntity;
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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class DiagnosisAssistantResourceTest {

    private static final String DOCTOR_EMAIL = "diagtest-doctor@statusscope.local";
    private static final String ADMIN_EMAIL = "diagtest-admin@statusscope.local";
    private static final UUID HOSPITAL_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID REGION_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID DISEASE_ID = UUID.fromString("60000000-0000-0000-0000-000000000004");
    private static final UUID OUTBREAK_ID = UUID.fromString("81000000-0000-0000-0000-000000000001");

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

        if (userRepository.findByEmail(DOCTOR_EMAIL).isEmpty()) {
            var doctorRole = roleRepository.findByCode("DOCTOR").orElseThrow();
            User doctor = new User();
            doctor.setId(UUID.randomUUID());
            doctor.setFullName("Test Doctor");
            doctor.setEmail(DOCTOR_EMAIL);
            doctor.setExternalAuthId("test-doctor-ext");
            doctor.setStatus(UserStatus.ACTIVE);
            doctor.setHospitalId(HOSPITAL_ID);
            doctor.setRoles(Set.of(doctorRole));
            userRepository.create(doctor);
        }

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
            RegionEntity region = entityManager.getReference(RegionEntity.class, REGION_ID);

            OutbreakEntity outbreak = new OutbreakEntity();
            outbreak.setId(OUTBREAK_ID);
            outbreak.setDisease(disease);
            outbreak.setRegion(region);
            outbreak.setCaseCount(12);
            outbreak.setStatus("ACTIVE");
            outbreak.setStartedAt(LocalDateTime.now().minusDays(3));
            outbreak.setCreatedAt(LocalDateTime.now());
            outbreak.setUpdatedAt(LocalDateTime.now());
            entityManager.persist(outbreak);
        }
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
                .body("contextUsed.regionName", notNullValue())
                .body("contextUsed.outbreaks[0].diseaseName", equalTo("COVID-19"))
                .body("contextUsed.outbreaks[0].caseCount", equalTo(12));
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
}
