package com.itesm.interfaces.rest;

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
import com.itesm.infrastructure.persistence.entity.UserEntity;
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
class AdminOperationalResourceTest {

    private static final String ADMIN_EMAIL = "ops-admin@statusscope.local";
    private static final String DOCTOR_EMAIL = "ops-doctor@statusscope.local";
    private static final UUID HOSPITAL_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID DISEASE_ID = UUID.fromString("60000000-0000-0000-0000-000000000004");
    private static final UUID OUTBREAK_ID_1 = UUID.fromString("85000000-0000-0000-0000-000000000001");
    private static final UUID OUTBREAK_ID_2 = UUID.fromString("85000000-0000-0000-0000-000000000002");
    private static final UUID ALERT_ID = UUID.fromString("85000000-0000-0000-0000-000000000101");
    private static final UUID SNAPSHOT_ID = UUID.fromString("85000000-0000-0000-0000-000000000201");
    private static final UUID SEEDED_RECOMMENDATION_ID = UUID.fromString("24000000-0000-0000-0000-000000000001");
    private static final UUID SEEDED_INVENTORY_ITEM_ID = UUID.fromString("23000000-0000-0000-0000-000000000005");
    private static final UUID SEEDED_CONTACT_ID = UUID.fromString("25000000-0000-0000-0000-000000000001");
    private static final UUID SEEDED_GROUP_ID = UUID.fromString("26000000-0000-0000-0000-000000000001");

    @Inject UserRepository userRepository;
    @Inject RoleRepository roleRepository;
    @Inject EntityManager entityManager;

    @BeforeEach
    @Transactional
    void seedOperationalSignals() {
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
        seedOutbreak(OUTBREAK_ID_1, disease, nearestMunicipality, 140);
        seedOutbreak(OUTBREAK_ID_2, disease, nearestMunicipality, 95);
        seedAlert(nearestMunicipality);
        seedLatestSnapshot();
        seedEventsAndEvaluations(doctor);
    }

    @Test
    void shouldListSeededRecommendationsForHospitalAdmin() {
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
                .body("find { it.id == '%s' }.availableActions.code".formatted(SEEDED_RECOMMENDATION_ID), hasItem("ASSIGN_TASK"))
                .body("find { it.id == '%s' }.allowedStatusTransitions".formatted(SEEDED_RECOMMENDATION_ID), hasItem("ACCEPTED"))
                .body("find { it.id == '%s' }.primaryDepartment.label".formatted(SEEDED_RECOMMENDATION_ID), equalTo("Intensive Care Unit"));
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
                .body("findAll { it.title == 'Replenish Critical Protective and Respiratory Supplies' }.size()", equalTo(1));
    }

    @Test
    void shouldPersistWorkflowActionsAndReturnThemInDetail() {
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
                .patch("/admin/recommendations/{id}/status", SEEDED_RECOMMENDATION_ID)
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
                .post("/admin/recommendations/{id}/tasks", SEEDED_RECOMMENDATION_ID)
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
                .post("/admin/recommendations/{id}/notifications", SEEDED_RECOMMENDATION_ID)
                .then()
                .statusCode(201)
                .body("status", equalTo("SENT"));

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
                .post("/admin/recommendations/{id}/supply-requests", SEEDED_RECOMMENDATION_ID)
                .then()
                .statusCode(201)
                .body("status", equalTo("REQUESTED"));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .get("/admin/recommendations/{id}", SEEDED_RECOMMENDATION_ID)
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
                .body("auditTrail.eventType", hasItem("NOTIFICATION_SENT"))
                .body("auditTrail.eventType", hasItem("SUPPLY_REQUESTED"));
    }

    @Test
    void shouldExposeOperationalDirectoryAndResourceSupportEndpoints() {
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
                .get("/admin/recommendations/{id}/workflow-options", SEEDED_RECOMMENDATION_ID)
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
                          "capturedAt": "2026-05-12T12:00:00",
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

    private void seedOutbreak(UUID outbreakId, DiseaseEntity disease, MunicipalityEntity municipality, int caseCount) {
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
        outbreak.setStartedAt(LocalDateTime.now().minusDays(2));
        outbreak.setUpdatedAt(LocalDateTime.now());
        if (isNew) {
            entityManager.persist(outbreak);
        }
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
}
