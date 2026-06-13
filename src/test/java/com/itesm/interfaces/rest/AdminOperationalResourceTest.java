package com.itesm.interfaces.rest;

import com.itesm.application.port.out.AssistantChatGateway;
import com.itesm.application.port.out.AssistantChatMessage;
import com.itesm.domain.models.User;
import com.itesm.domain.models.UserStatus;
import com.itesm.domain.repository.RoleRepository;
import com.itesm.domain.repository.UserRepository;
import com.itesm.infrastructure.persistence.entity.AlertEntity;
import com.itesm.infrastructure.persistence.entity.DiseaseEntity;
import com.itesm.infrastructure.persistence.entity.EventEntity;
import com.itesm.infrastructure.persistence.entity.HospitalEntity;
import com.itesm.infrastructure.persistence.entity.HospitalResourceSnapshotEntity;
import com.itesm.infrastructure.persistence.entity.MunicipalityEntity;
import com.itesm.infrastructure.persistence.entity.OutbreakEntity;
import com.itesm.infrastructure.persistence.entity.PatientEntity;
import com.itesm.infrastructure.persistence.entity.PatientEvaluationEntity;
import com.itesm.infrastructure.persistence.entity.SpecialtyEntity;
import com.itesm.infrastructure.persistence.entity.StateEntity;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class AdminOperationalResourceTest {

    private static final String ADMIN_EMAIL = "ops-admin@statusscope.local";
    private static final String DOCTOR_EMAIL = "ops-doctor@statusscope.local";
    private static final UUID HOSPITAL_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID DISEASE_ID = UUID.fromString("60000000-0000-0000-0000-000000000004");
    private static final UUID MEASLES_DISEASE_ID = UUID.fromString("86000000-0000-0000-0000-000000000901");
    private static final UUID DENGUE_DISEASE_ID = UUID.fromString("86000000-0000-0000-0000-000000000902");
    private static final UUID OUTBREAK_ID_1 = UUID.fromString("85000000-0000-0000-0000-000000000001");
    private static final UUID OUTBREAK_ID_2 = UUID.fromString("85000000-0000-0000-0000-000000000002");
    private static final UUID MUNICIPAL_MEASLES_OUTBREAK_ID = UUID.fromString("85000000-0000-0000-0000-000000000003");
    private static final UUID STATE_COVID_OUTBREAK_ID = UUID.fromString("85000000-0000-0000-0000-000000000004");
    private static final UUID STATE_DENGUE_OUTBREAK_ID = UUID.fromString("85000000-0000-0000-0000-000000000005");
    private static final UUID OLD_MUNICIPAL_DENGUE_OUTBREAK_ID = UUID.fromString("85000000-0000-0000-0000-000000000006");
    private static final UUID ALERT_ID = UUID.fromString("85000000-0000-0000-0000-000000000101");
    private static final UUID SNAPSHOT_ID = UUID.fromString("85000000-0000-0000-0000-000000000201");
    private static final UUID SEEDED_INVENTORY_ITEM_ID = UUID.fromString("23000000-0000-0000-0000-000000000005");
    private static final UUID SEEDED_CONTACT_ID = UUID.fromString("25000000-0000-0000-0000-000000000001");
    private static final UUID SEEDED_GROUP_ID = UUID.fromString("26000000-0000-0000-0000-000000000001");

    @InjectMock
    AssistantChatGateway assistantChatGateway;

    @Inject UserRepository userRepository;
    @Inject RoleRepository roleRepository;
    @Inject EntityManager entityManager;

    @BeforeEach
    @Transactional
    void seedOperationalSignals() {
        Mockito.when(assistantChatGateway.chat(Mockito.anyList()))
                .thenAnswer(invocation -> {
                    List<AssistantChatMessage> messages = invocation.getArgument(0);
                    String prompt = messages.isEmpty() ? "" : messages.get(0).getContent();
                    String draftTitle = extractPromptValue(prompt, "- Current draft title: ", "Operational recommendation");
                    return llmRecommendationResponse(draftTitle);
                });

        User admin = userRepository.findByEmail(ADMIN_EMAIL).orElseGet(() -> {
            var adminRole = roleRepository.findByCode("HOSPITAL_ADMIN").orElseThrow();
            User newAdmin = new User();
            newAdmin.setId(UUID.randomUUID());
            newAdmin.setFullName("Operational Admin");
            newAdmin.setEmail(ADMIN_EMAIL);
            newAdmin.setExternalAuthId("ops-admin-ext");
            newAdmin.setStatus(UserStatus.ACTIVE);
            newAdmin.setHospitalId(HOSPITAL_ID);
            newAdmin.setRoles(Set.of(adminRole));
            return userRepository.create(newAdmin);
        });

        User doctor = userRepository.findByEmail(DOCTOR_EMAIL).orElseGet(() -> {
            var doctorRole = roleRepository.findByCode("DOCTOR").orElseThrow();
            User newDoctor = new User();
            newDoctor.setId(UUID.randomUUID());
            newDoctor.setFullName("Operational Doctor");
            newDoctor.setEmail(DOCTOR_EMAIL);
            newDoctor.setExternalAuthId("ops-doctor-ext");
            newDoctor.setStatus(UserStatus.ACTIVE);
            newDoctor.setHospitalId(HOSPITAL_ID);
            newDoctor.setRoles(Set.of(doctorRole));
            return userRepository.create(newDoctor);
        });

        MunicipalityEntity nearestMunicipality = entityManager.createQuery("""
                select m
                from MunicipalityEntity m
                where m.latitude is not null and m.longitude is not null
                order by abs(m.latitude - :lat) + abs(m.longitude - :lon)
                """, MunicipalityEntity.class)
                .setParameter("lat", new BigDecimal("25.6866142"))
                .setParameter("lon", new BigDecimal("-100.3161126"))
                .setMaxResults(1)
                .getSingleResult();

        DiseaseEntity disease = entityManager.getReference(DiseaseEntity.class, DISEASE_ID);
        DiseaseEntity measles = ensureTestDisease(MEASLES_DISEASE_ID, "MEASLES_TEST", "Measles");
        DiseaseEntity dengue = ensureTestDisease(DENGUE_DISEASE_ID, "DENGUE_TEST", "Dengue");
        seedOutbreak(OUTBREAK_ID_1, disease, nearestMunicipality, 140);
        seedOutbreak(OUTBREAK_ID_2, disease, nearestMunicipality, 95);
        seedOutbreak(MUNICIPAL_MEASLES_OUTBREAK_ID, measles, nearestMunicipality, 80);
        seedOutbreak(OLD_MUNICIPAL_DENGUE_OUTBREAK_ID, dengue, nearestMunicipality, 2000, 45);
        seedStateOutbreak(STATE_COVID_OUTBREAK_ID, disease, nearestMunicipality.getState(), 20000);
        seedStateOutbreak(STATE_DENGUE_OUTBREAK_ID, dengue, nearestMunicipality.getState(), 10000);
        seedAlert(nearestMunicipality);
        seedLatestSnapshot();
        seedEventsAndEvaluations(doctor);

        resetOperationalRecommendations();
    }

    @Test
    void shouldListGeneratedRecommendationsForHospitalAdmin() {
        refreshRecommendations();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .get("/admin/recommendations")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0))
                .body("title", hasItem("ICU Capacity Critical - Activate Surge Protocol"))
                .body("type", hasItem("LOCAL_EPIDEMIOLOGY"))
                .body("find { it.title == 'ICU Capacity Critical - Activate Surge Protocol' }.availableActions.code", hasItem("ASSIGN_TASK"))
                .body("find { it.title == 'ICU Capacity Critical - Activate Surge Protocol' }.allowedStatusTransitions", hasItem("ACCEPTED"))
                .body("find { it.title == 'ICU Capacity Critical - Activate Surge Protocol' }.primaryDepartment.label", equalTo("Intensive Care Unit"))
                .body("find { it.title == 'ICU Capacity Critical - Activate Surge Protocol' }.translations.es.title", equalTo("Activar protocolo de expansion UCI"));
    }

    @Test
    void shouldRefreshRecommendationsWithoutDuplicatingOpenItems() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .post("/admin/recommendations/refresh")
                .then()
                .statusCode(200)
                .body("generated", greaterThan(0));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .post("/admin/recommendations/refresh")
                .then()
                .statusCode(200)
                .body("generated", equalTo(0));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .get("/admin/recommendations")
                .then()
                .statusCode(200)
                .body("findAll { it.title == 'Expand Monitored Bed Capacity' }.size()", equalTo(1))
                .body("findAll { it.title == 'Increase Emergency Physician Staffing' }.size()", equalTo(1))
                .body("findAll { it.title == 'Replenish Critical Protective and Respiratory Supplies' }.size()", equalTo(1))
                .body("find { it.title == 'Expand Monitored Bed Capacity' }.createdByMode", equalTo("LLM_ASSISTED"));
    }

    @Test
    void shouldGenerateUniqueHospitalAndMunicipalEpidemiologyRecommendations() {
        refreshRecommendations();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .get("/admin/recommendations")
                .then()
                .statusCode(200)
                .body("findAll { it.type.startsWith('EPIDEMIOLOGY') }.size()", equalTo(2))
                .body("find { it.type == 'EPIDEMIOLOGY_HOSPITAL' }.title", containsString("COVID-19"))
                .body("find { it.type == 'EPIDEMIOLOGY_MUNICIPAL' }.title", containsString("Measles"))
                .body("find { it.type == 'EPIDEMIOLOGY_MUNICIPAL' }.title", not(containsString("Dengue")))
                .body("find { it.type == 'EPIDEMIOLOGY_MUNICIPAL' }.title", containsString("surrounding municipalities"))
                .body("find { it.type == 'EPIDEMIOLOGY_STATE' }", equalTo(null))
                .body("find { it.type == 'EPIDEMIOLOGY_HOSPITAL' }.createdByMode", equalTo("LLM_ASSISTED"))
                .body("find { it.type == 'EPIDEMIOLOGY_MUNICIPAL' }.createdByMode", equalTo("LLM_ASSISTED"))
                .body("find { it.type == 'EPIDEMIOLOGY_HOSPITAL' }.severity", equalTo("CRITICAL"))
                .body("find { it.type == 'EPIDEMIOLOGY_MUNICIPAL' }.severity", equalTo("CRITICAL"));
    }

    @Test
    void shouldPersistWorkflowActionsAndReturnThemInDetail() {
        String recommendationId = generatedRecommendationId("ICU Capacity Critical - Activate Surge Protocol");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .body("""
                        {
                          "status": "ACCEPTED"
                        }
                        """)
                .when()
                .patch("/admin/recommendations/{id}/status", recommendationId)
                .then()
                .statusCode(204);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .body("""
                        {
                          "ownerContactId": "%s",
                          "ownerGroupId": "%s",
                          "ownerLabel": "Respiratory Ops Team",
                          "departmentLabel": "ICU",
                          "priority": "HIGH",
                          "notes": "Prepare monitored overflow beds."
                        }
                        """.formatted(SEEDED_CONTACT_ID, SEEDED_GROUP_ID))
                .when()
                .post("/admin/recommendations/{id}/tasks", recommendationId)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("ownerContactId", equalTo(SEEDED_CONTACT_ID.toString()))
                .body("ownerGroupId", equalTo(SEEDED_GROUP_ID.toString()));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .body("""
                        {
                          "audienceGroupId": "%s",
                          "audienceLabel": "Charge Nurses",
                          "message": "Prepare the ICU surge protocol."
                        }
                        """.formatted(SEEDED_GROUP_ID))
                .when()
                .post("/admin/recommendations/{id}/notifications", recommendationId)
                .then()
                .statusCode(201)
                .body("status", equalTo("FAILED"));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .body("""
                        {
                          "inventoryItemId": "%s",
                          "supplyTypeLabel": "Isolation gowns",
                          "quantity": 300,
                          "unit": "units",
                          "destination": "Central Supply",
                          "suggestedSupplier": "Proveedor Norte"
                        }
                        """.formatted(SEEDED_INVENTORY_ITEM_ID))
                .when()
                .post("/admin/recommendations/{id}/supply-requests", recommendationId)
                .then()
                .statusCode(201)
                .body("status", equalTo("REQUESTED"));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .get("/admin/recommendations/{id}", recommendationId)
                .then()
                .statusCode(200)
                .body("status", equalTo("ASSIGNED"))
                .body("availableActions.code", hasItem("ASSIGN_TASK"))
                .body("primaryDepartment.label", equalTo("Intensive Care Unit"))
                .body("tasks.size()", greaterThan(0))
                .body("tasks[0].ownerContactId", equalTo(SEEDED_CONTACT_ID.toString()))
                .body("notifications.size()", greaterThan(0))
                .body("notifications[0].audienceGroupId", equalTo(SEEDED_GROUP_ID.toString()))
                .body("supplyRequests.size()", greaterThan(0))
                .body("supplyRequests[0].inventoryItemId", equalTo(SEEDED_INVENTORY_ITEM_ID.toString()))
                .body("auditTrail.eventType", hasItem("STATUS_CHANGED"))
                .body("auditTrail.eventType", hasItem("TASK_CREATED"))
                .body("auditTrail.eventType", hasItem("NOTIFICATION_EMAIL_FAILED"))
                .body("auditTrail.eventType", hasItem("SUPPLY_REQUESTED"));
    }

    @Test
    void shouldManageOperationalContactsAndRecordEmailDeliveryEvidence() {
        String contactId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .body("""
                        {
                          "displayName": "Jefa de Enfermeria",
                          "roleLabel": "Chief Nurse",
                          "departmentCode": "ICU",
                          "email": "nursing-lead@hgz21.local",
                          "assignable": true,
                          "notifiable": true,
                          "availabilityStatus": "ACTIVE"
                        }
                        """)
                .when()
                .post("/admin/operational-contacts")
                .then()
                .statusCode(201)
                .body("contactChannel", equalTo("EMAIL"))
                .body("contactValue", equalTo("nursing-lead@hgz21.local"))
                .extract()
                .path("id");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .body("""
                        {
                          "displayName": "Contacto invalido",
                          "roleLabel": "Nurse",
                          "email": "not-an-email",
                          "assignable": true,
                          "notifiable": true,
                          "availabilityStatus": "ACTIVE"
                        }
                        """)
                .when()
                .post("/admin/operational-contacts")
                .then()
                .statusCode(400);

        String recommendationId = generatedRecommendationId("ICU Capacity Critical - Activate Surge Protocol");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .body("""
                        {
                          "ownerContactId": "%s",
                          "departmentLabel": "ICU",
                          "priority": "CRITICAL",
                          "notes": "Activar protocolo de expansion UCI.",
                          "language": "es"
                        }
                        """.formatted(contactId))
                .when()
                .post("/admin/recommendations/{id}/tasks", recommendationId)
                .then()
                .statusCode(201)
                .body("ownerContactId", equalTo(contactId))
                .body("ownerLabel", equalTo("Jefa de Enfermeria"));

        given()
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .get("/admin/recommendations/{id}", recommendationId)
                .then()
                .statusCode(200)
                .body("notifications.find { it.audienceContactId == '%s' }.deliveryChannel".formatted(contactId), equalTo("EMAIL"))
                .body("notifications.find { it.audienceContactId == '%s' }.status".formatted(contactId), equalTo("SENT"))
                .body("notifications.find { it.audienceContactId == '%s' }.recipientSummary.sent".formatted(contactId), equalTo(1))
                .body("auditTrail.eventType", hasItem("TASK_EMAIL_SENT"));
    }

    @Test
    void shouldExposeOperationalDirectoryAndResourceSupportEndpoints() {
        String recommendationId = generatedRecommendationId("ICU Capacity Critical - Activate Surge Protocol");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .get("/admin/operational-contacts?assignable=true")
                .then()
                .statusCode(200)
                .body("displayName", hasItem("Dr. Elena Ramirez"));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .get("/admin/operational-groups?notifiable=true")
                .then()
                .statusCode(200)
                .body("groupName", hasItem("ICU Response Team"));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .get("/admin/recommendations/{id}/workflow-options", recommendationId)
                .then()
                .statusCode(200)
                .body("availableActions.code", hasItem("ASSIGN_TASK"))
                .body("allowedStatusTransitions", hasItem("ACCEPTED"));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .get("/admin/resources/configuration")
                .then()
                .statusCode(200)
                .body("section", equalTo("configuration"))
                .body("data.inventory.size()", greaterThan(0));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .get("/admin/resources/operational-roster")
                .then()
                .statusCode(200)
                .body("section", equalTo("operational-roster"))
                .body("data.displayName", hasItem("Dr. Elena Ramirez"));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .get("/admin/resources/inventory/{id}/movements", SEEDED_INVENTORY_ITEM_ID)
                .then()
                .statusCode(200)
                .body("section", equalTo("inventory-movements"))
                .body("data.size()", greaterThan(0));

        String directSupplyRequestId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .body("""
                        {
                          "supplyTypeLabel": "Isolation Gowns",
                          "quantity": 50,
                          "unit": "units",
                          "destination": "Central Supply",
                          "suggestedSupplier": "Preferred Supplier",
                          "priority": "HIGH"
                        }
                        """)
                .when()
                .post("/admin/resources/inventory/{id}/supply-requests", SEEDED_INVENTORY_ITEM_ID)
                .then()
                .statusCode(201)
                .body("recommendationId", nullValue())
                .body("inventoryItemId", equalTo(SEEDED_INVENTORY_ITEM_ID.toString()))
                .body("status", equalTo("REQUESTED"))
                .extract().path("id");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .get("/admin/resources/inventory/{id}/movements", SEEDED_INVENTORY_ITEM_ID)
                .then()
                .statusCode(200)
                .body("data.relatedSupplyRequestId", hasItem(directSupplyRequestId));
    }

    @Test
    void shouldReadAndUpdateHospitalResources() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .get("/admin/resources/summary")
                .then()
                .statusCode(200)
                .body("section", equalTo("summary"))
                .body("data.totalBeds", equalTo(240))
                .body("data.availableBeds", equalTo(18));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .body("""
                        {
                          "capturedAt": "2099-12-31T12:00:00",
                          "totalBeds": 240,
                          "availableBeds": 24,
                          "icuTotalBeds": 20,
                          "icuAvailableBeds": 2,
                          "isolationRoomsTotal": 12,
                          "isolationRoomsAvailable": 2,
                          "oxygenCapacityUnits": 500,
                          "oxygenAvailableUnits": 150,
                          "doctorsOnShift": 20,
                          "nursesOnShift": 60,
                          "specialistsOnShift": 8,
                          "source": "MANUAL"
                        }
                        """)
                .when()
                .put("/admin/resources/summary")
                .then()
                .statusCode(200)
                .body("availableBeds", equalTo(24));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .get("/admin/resources/summary")
                .then()
                .statusCode(200)
                .body("data.availableBeds", equalTo(24));
    }

    @Test
    void shouldCreateAndDeleteHospitalResourcesAndRejectDeletingReferencedInventory() {
        String createdDepartmentId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .body("""
                        {
                          "departmentCode": "OBS_UNIT",
                          "departmentName": "Observation Unit",
                          "levelLabel": "Level 2",
                          "totalBeds": 18,
                          "occupiedBeds": 9,
                          "status": "STABLE",
                          "notes": "Created from CRUD resource test."
                        }
                        """)
                .when()
                .post("/admin/resources/departments")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("departmentName", equalTo("Observation Unit"))
                .extract()
                .path("id");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .delete("/admin/resources/departments/{id}", createdDepartmentId)
                .then()
                .statusCode(204);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .get("/admin/resources/departments")
                .then()
                .statusCode(200)
                .body("data.id", not(hasItem(createdDepartmentId)));

        String createdStaffingId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .body("""
                        {
                          "roleCode": "RESP_THERAPIST",
                          "roleName": "Respiratory Therapist",
                          "headcount": 14,
                          "onShiftCount": 6,
                          "onCallCount": 4,
                          "standbyCount": 2
                        }
                        """)
                .when()
                .post("/admin/resources/staffing")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("roleName", equalTo("Respiratory Therapist"))
                .extract()
                .path("id");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .delete("/admin/resources/staffing/{id}", createdStaffingId)
                .then()
                .statusCode(204);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .get("/admin/resources/staffing")
                .then()
                .statusCode(200)
                .body("data.id", not(hasItem(createdStaffingId)));

        String createdInventoryId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .body("""
                        {
                          "itemCode": "PPE_KIT",
                          "itemName": "PPE Test Kit",
                          "category": "Protective Equipment",
                          "location": "Overflow Storage",
                          "currentQuantity": 35,
                          "capacityQuantity": 80,
                          "unit": "kits",
                          "criticalThreshold": 10,
                          "targetQuantity": 60,
                          "status": "ADEQUATE"
                        }
                        """)
                .when()
                .post("/admin/resources/inventory")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("itemName", equalTo("PPE Test Kit"))
                .extract()
                .path("id");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .delete("/admin/resources/inventory/{id}", createdInventoryId)
                .then()
                .statusCode(204);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .get("/admin/resources/inventory")
                .then()
                .statusCode(200)
                .body("data.id", not(hasItem(createdInventoryId)));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .delete("/admin/resources/inventory/{id}", SEEDED_INVENTORY_ITEM_ID)
                .then()
                .statusCode(409)
                .body("code", equalTo("CONFLICT"));
    }

    private void seedOutbreak(UUID outbreakId, DiseaseEntity disease, MunicipalityEntity municipality, int caseCount) {
        seedOutbreak(outbreakId, disease, municipality, caseCount, 2);
    }

    private void seedOutbreak(UUID outbreakId, DiseaseEntity disease, MunicipalityEntity municipality, int caseCount, int startedDaysAgo) {
        OutbreakEntity outbreak = entityManager.find(OutbreakEntity.class, outbreakId);
        boolean isNew = outbreak == null;
        if (outbreak == null) {
            outbreak = new OutbreakEntity();
            outbreak.setId(outbreakId);
            outbreak.setCreatedAt(LocalDateTime.now());
        }
        outbreak.setDisease(disease);
        outbreak.setScope("MUNICIPALITY");
        outbreak.setMunicipality(municipality);
        outbreak.setCaseCount(caseCount);
        outbreak.setConfirmationStatus("CONFIRMED");
        outbreak.setStatus("ACTIVE");
        outbreak.setStartedAt(LocalDateTime.now().minusDays(startedDaysAgo));
        outbreak.setUpdatedAt(LocalDateTime.now());
        if (isNew) {
            entityManager.persist(outbreak);
        }
    }

    private void seedStateOutbreak(UUID outbreakId, DiseaseEntity disease, StateEntity state, int caseCount) {
        OutbreakEntity outbreak = entityManager.find(OutbreakEntity.class, outbreakId);
        boolean isNew = outbreak == null;
        if (outbreak == null) {
            outbreak = new OutbreakEntity();
            outbreak.setId(outbreakId);
            outbreak.setCreatedAt(LocalDateTime.now());
        }
        outbreak.setDisease(disease);
        outbreak.setScope("STATE");
        outbreak.setMunicipality(null);
        outbreak.setState(state);
        outbreak.setCaseCount(caseCount);
        outbreak.setConfirmationStatus("CONFIRMED");
        outbreak.setStatus("ACTIVE");
        outbreak.setStartedAt(LocalDateTime.now().minusDays(2));
        outbreak.setUpdatedAt(LocalDateTime.now());
        if (isNew) {
            entityManager.persist(outbreak);
        }
    }

    private DiseaseEntity ensureTestDisease(UUID diseaseId, String code, String name) {
        DiseaseEntity disease = entityManager.find(DiseaseEntity.class, diseaseId);
        boolean isNew = disease == null;
        if (disease == null) {
            disease = new DiseaseEntity();
            disease.setId(diseaseId);
            disease.setCreatedAt(LocalDateTime.now());
        }
        SpecialtyEntity specialty = entityManager.getReference(
                SpecialtyEntity.class,
                UUID.fromString("50000000-0000-0000-0000-000000000004"));
        disease.setCode(code);
        disease.setName(name);
        disease.setPrimarySpecialty(specialty);
        disease.setUpdatedAt(LocalDateTime.now());
        if (isNew) {
            entityManager.persist(disease);
        }
        return disease;
    }

    private void resetOperationalRecommendations() {
        entityManager.createQuery("delete from SupplyRequestEntity s where s.hospital.id = :hospitalId")
                .setParameter("hospitalId", HOSPITAL_ID)
                .executeUpdate();
        entityManager.createQuery("delete from OperationalNotificationEntity n where n.hospital.id = :hospitalId")
                .setParameter("hospitalId", HOSPITAL_ID)
                .executeUpdate();
        entityManager.createQuery("delete from OperationalTaskEntity t where t.hospital.id = :hospitalId")
                .setParameter("hospitalId", HOSPITAL_ID)
                .executeUpdate();
        entityManager.createQuery("delete from OperationalRecommendationAuditEntity a where a.recommendation.hospital.id = :hospitalId")
                .setParameter("hospitalId", HOSPITAL_ID)
                .executeUpdate();
        entityManager.createQuery("delete from OperationalRecommendationEntity r where r.hospital.id = :hospitalId")
                .setParameter("hospitalId", HOSPITAL_ID)
                .executeUpdate();
    }

    private void refreshRecommendations() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .post("/admin/recommendations/refresh")
                .then()
                .statusCode(200)
                .body("generated", greaterThan(0));
    }

    private String generatedRecommendationId(String title) {
        refreshRecommendations();
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .get("/admin/recommendations")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("find { it.title == '%s' }.id".formatted(title));
    }

    private void seedAlert(MunicipalityEntity municipality) {
        AlertEntity alert = entityManager.find(AlertEntity.class, ALERT_ID);
        if (alert == null) {
            alert = new AlertEntity();
            alert.setId(ALERT_ID);
            alert.setOutbreak(entityManager.getReference(OutbreakEntity.class, OUTBREAK_ID_1));
            alert.setSeverity("HIGH");
            alert.setMessage("Respiratory outbreak escalation detected near " + municipality.getName());
            alert.setCreatedAt(LocalDateTime.now());
            entityManager.persist(alert);
        }
    }

    private void seedLatestSnapshot() {
        HospitalResourceSnapshotEntity snapshot = entityManager.find(HospitalResourceSnapshotEntity.class, SNAPSHOT_ID);
        boolean isNew = snapshot == null;
        if (snapshot == null) {
            snapshot = new HospitalResourceSnapshotEntity();
            snapshot.setId(SNAPSHOT_ID);
            snapshot.setCreatedAt(LocalDateTime.now());
        }
        snapshot.setHospital(entityManager.getReference(HospitalEntity.class, HOSPITAL_ID));
        snapshot.setCapturedAt(LocalDateTime.now().plusMinutes(1));
        snapshot.setTotalBeds(240);
        snapshot.setAvailableBeds(18);
        snapshot.setIcuTotalBeds(20);
        snapshot.setIcuAvailableBeds(1);
        snapshot.setIsolationRoomsTotal(12);
        snapshot.setIsolationRoomsAvailable(2);
        snapshot.setOxygenCapacityUnits(500);
        snapshot.setOxygenAvailableUnits(120);
        snapshot.setDoctorsOnShift(20);
        snapshot.setNursesOnShift(60);
        snapshot.setSpecialistsOnShift(8);
        snapshot.setSource("MANUAL");
        if (isNew) {
            entityManager.persist(snapshot);
        }
    }

    private void seedEventsAndEvaluations(User doctor) {
        HospitalEntity hospital = entityManager.getReference(HospitalEntity.class, HOSPITAL_ID);
        DiseaseEntity disease = entityManager.getReference(DiseaseEntity.class, DISEASE_ID);
        UserEntity doctorEntity = entityManager.getReference(UserEntity.class, doctor.getId());

        for (int i = 0; i < 12; i++) {
            UUID patientId = UUID.fromString("86000000-0000-0000-0000-%012d".formatted(i + 1));
            UUID eventId = UUID.fromString("86100000-0000-0000-0000-%012d".formatted(i + 1));
            UUID evaluationId = UUID.fromString("86200000-0000-0000-0000-%012d".formatted(i + 1));

            PatientEntity patient = entityManager.find(PatientEntity.class, patientId);
            boolean newPatient = patient == null;
            if (patient == null) {
                patient = new PatientEntity();
                patient.setId(patientId);
                patient.setCreatedAt(LocalDateTime.now());
            }
            patient.setHospital(hospital);
            patient.setFullName("Operational Patient " + i);
            patient.setSex("female");
            patient.setBirthDate(LocalDate.now().minusYears(30));
            patient.setWeightKg(new BigDecimal("70.00"));
            patient.setHeightCm(new BigDecimal("165.00"));
            patient.setPostalCode("64000");
            patient.setUpdatedAt(LocalDateTime.now());
            if (newPatient) {
                entityManager.persist(patient);
            }

            EventEntity event = entityManager.find(EventEntity.class, eventId);
            boolean newEvent = event == null;
            if (event == null) {
                event = new EventEntity();
                event.setId(eventId);
                event.setCreatedAt(LocalDateTime.now());
            }
            event.setPatient(patient);
            event.setDisease(disease);
            event.setPrimaryDoctor(doctorEntity);
            event.setStatus("ACTIVE");
            event.setStartedAt(LocalDateTime.now().minusHours(12));
            event.setUpdatedAt(LocalDateTime.now());
            if (newEvent) {
                entityManager.persist(event);
            }

            PatientEvaluationEntity evaluation = entityManager.find(PatientEvaluationEntity.class, evaluationId);
            boolean newEvaluation = evaluation == null;
            if (evaluation == null) {
                evaluation = new PatientEvaluationEntity();
                evaluation.setId(evaluationId);
                evaluation.setCreatedAt(LocalDateTime.now());
            }
            evaluation.setPatient(patient);
            evaluation.setDoctor(doctorEntity);
            evaluation.setEvent(event);
            evaluation.setStatus("IN_PROGRESS");
            evaluation.setSymptomsText("Fever, cough, shortness of breath.");
            evaluation.setClinicalNotes("Operational backend test signal.");
            evaluation.setUpdatedAt(LocalDateTime.now());
            if (newEvaluation) {
                entityManager.persist(evaluation);
            }
        }
    }

    private String extractPromptValue(String prompt, String prefix, String fallback) {
        int start = prompt.indexOf(prefix);
        if (start < 0) {
            return fallback;
        }
        int valueStart = start + prefix.length();
        int valueEnd = prompt.indexOf('\n', valueStart);
        if (valueEnd < 0) {
            valueEnd = prompt.length();
        }
        String value = prompt.substring(valueStart, valueEnd).trim();
        return value.isBlank() ? fallback : value;
    }

    private String llmRecommendationResponse(String title) {
        boolean municipalEpidemiology = title.startsWith("Municipal epidemiological signal - ");
        String englishTitle = municipalEpidemiology
                ? title.replace("Municipal epidemiological signal - ", "") + " readiness in surrounding municipalities"
                : title;
        String spanishTitle = municipalEpidemiology
                ? "Preparacion ante " + title.replace("Municipal epidemiological signal - ", "") + " en municipios circundantes"
                : "ICU Capacity Critical - Activate Surge Protocol".equals(title)
                ? "Activar protocolo de expansion UCI"
                : "ES " + title;
        return """
                {
                  "translations": {
                    "en": {
                      "title": "%s",
                      "description": "Operational outlook indicates elevated pressure on this resource area. Activate the documented response pathway before the next surge window.",
                      "expectedImpact": "Improve coordination and reduce avoidable operational bottlenecks",
                      "urgencyWindow": "Within 24 hours",
                      "rationale": [
                        "Current hospital utilization and nearby outbreak activity point to growing operational pressure.",
                        "Earlier intervention reduces the risk of escalation and reactive staffing or supply changes."
                      ],
                      "recommendedActions": [
                        "Review the assigned unit readiness against the current operational protocol.",
                        "Prepare the next response step with the responsible hospital lead."
                      ]
                    },
                    "es": {
                      "title": "%s",
                      "description": "El panorama operativo indica presion elevada sobre esta area de recursos. Active la ruta de respuesta documentada antes de la siguiente ventana de incremento.",
                      "expectedImpact": "Mejorar coordinacion y reducir cuellos de botella operativos evitables",
                      "urgencyWindow": "Dentro de 24 horas",
                      "rationale": [
                        "La utilizacion hospitalaria actual y la actividad de brotes cercanos apuntan a mayor presion operativa.",
                        "Intervenir antes reduce el riesgo de escalamiento y cambios reactivos de personal o suministros."
                      ],
                      "recommendedActions": [
                        "Revise la preparacion de la unidad asignada contra el protocolo operativo actual.",
                        "Prepare el siguiente paso de respuesta con el responsable hospitalario."
                      ]
                    }
                  }
                }
                """.formatted(jsonEscape(englishTitle), jsonEscape(spanishTitle));
    }

    private String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
