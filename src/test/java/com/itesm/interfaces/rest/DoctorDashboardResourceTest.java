package com.itesm.interfaces.rest;

import com.itesm.domain.models.User;
import com.itesm.domain.models.UserStatus;
import com.itesm.domain.repository.RoleRepository;
import com.itesm.domain.repository.UserRepository;
import com.itesm.infrastructure.persistence.entity.DiseaseEntity;
import com.itesm.infrastructure.persistence.entity.MunicipalityEntity;
import com.itesm.infrastructure.persistence.entity.OutbreakEntity;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class DoctorDashboardResourceTest {

    private static final String DOCTOR_EMAIL = "dashboard-doctor@statusscope.local";
    private static final String ADMIN_EMAIL = "dashboard-admin@statusscope.local";
    private static final UUID HOSPITAL_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID DISEASE_ID = UUID.fromString("60000000-0000-0000-0000-000000000004");
    private static final UUID OUTBREAK_ID = UUID.fromString("87000000-0000-0000-0000-000000000001");

    @Inject UserRepository userRepository;
    @Inject RoleRepository roleRepository;
    @Inject EntityManager entityManager;

    private UUID stateId;
    private String municipalityName;

    @BeforeEach
    @Transactional
    void seedStateMapOutbreak() {
        userRepository.findByEmail(DOCTOR_EMAIL).orElseGet(() -> {
            var doctorRole = roleRepository.findByCode("DOCTOR").orElseThrow();
            User doctor = new User();
            doctor.setId(UUID.randomUUID());
            doctor.setFullName("Dashboard Doctor");
            doctor.setEmail(DOCTOR_EMAIL);
            doctor.setExternalAuthId("dashboard-doctor-ext");
            doctor.setStatus(UserStatus.ACTIVE);
            doctor.setHospitalId(HOSPITAL_ID);
            doctor.setRoles(Set.of(doctorRole));
            return userRepository.create(doctor);
        });

        if (userRepository.findByEmail(ADMIN_EMAIL).isEmpty()) {
            var adminRole = roleRepository.findByCode("HOSPITAL_ADMIN").orElseThrow();
            User admin = new User();
            admin.setId(UUID.randomUUID());
            admin.setFullName("Dashboard Admin");
            admin.setEmail(ADMIN_EMAIL);
            admin.setExternalAuthId("dashboard-admin-ext");
            admin.setStatus(UserStatus.ACTIVE);
            admin.setHospitalId(HOSPITAL_ID);
            admin.setRoles(Set.of(adminRole));
            userRepository.create(admin);
        }

        MunicipalityEntity municipality = entityManager.createQuery("""
                select m
                from MunicipalityEntity m
                where m.latitude is not null
                  and m.longitude is not null
                  and m.state is not null
                order by m.name
                """, MunicipalityEntity.class)
                .setMaxResults(1)
                .getSingleResult();
        stateId = municipality.getState().getId();
        municipalityName = municipality.getName();

        DiseaseEntity disease = entityManager.getReference(DiseaseEntity.class, DISEASE_ID);
        OutbreakEntity outbreak = entityManager.find(OutbreakEntity.class, OUTBREAK_ID);
        boolean isNew = outbreak == null;
        if (outbreak == null) {
            outbreak = new OutbreakEntity();
            outbreak.setId(OUTBREAK_ID);
            outbreak.setCreatedAt(LocalDateTime.now());
        }
        outbreak.setDisease(disease);
        outbreak.setScope("MUNICIPALITY");
        outbreak.setMunicipality(municipality);
        outbreak.setState(null);
        outbreak.setCaseCount(77);
        outbreak.setConfirmationStatus("CONFIRMED");
        outbreak.setStatus("ACTIVE");
        outbreak.setStartedAt(LocalDateTime.now().minusDays(1));
        outbreak.setEndedAt(null);
        outbreak.setUpdatedAt(LocalDateTime.now());
        if (isNew) {
            entityManager.persist(outbreak);
        }
    }

    @Test
    void stateOutbreakMapShouldReturnMunicipalZonesForSelectedState() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .when()
                .get("/doctor/dashboard/map/states/{stateId}/outbreaks", stateId)
                .then()
                .statusCode(200)
                .body("zones.id", hasItem(OUTBREAK_ID.toString()))
                .body("zones.municipalityName", hasItem(municipalityName));
    }

    // ── Auth guards ────────────────────────────────────────────────────────────

    @Test
    void summaryShouldReturn401WhenNoToken() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/doctor/dashboard/summary")
                .then()
                .statusCode(401);
    }

    @Test
    void summaryShouldReturn200ForHospitalAdmin() {
        // /doctor/dashboard requires outbreaks.read, which HOSPITAL_ADMIN holds
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", ADMIN_EMAIL)
                .when()
                .get("/doctor/dashboard/summary")
                .then()
                .statusCode(200);
    }

    // ── Endpoint coverage ──────────────────────────────────────────────────────

    @Test
    void summaryShouldReturn200ForDoctor() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .when()
                .get("/doctor/dashboard/summary")
                .then()
                .statusCode(200)
                .body("hospitalName", notNullValue())
                .body("generatedAt", notNullValue());
    }

    @Test
    void metricsShouldReturn200ForDoctor() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .when()
                .get("/doctor/dashboard/metrics")
                .then()
                .statusCode(200)
                .body("metrics", notNullValue())
                .body("hospitalName", notNullValue());
    }

    @Test
    void mapShouldReturn200ForDoctor() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .when()
                .get("/doctor/dashboard/map")
                .then()
                .statusCode(200)
                .body("zones", notNullValue())
                .body("generatedAt", notNullValue());
    }

    @Test
    void stateMapShouldReturn200ForDoctor() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .when()
                .get("/doctor/dashboard/map/states")
                .then()
                .statusCode(200)
                .body("states", notNullValue());
    }

    @Test
    void diseaseCatalogShouldReturn200ForDoctor() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .when()
                .get("/doctor/dashboard/diseases")
                .then()
                .statusCode(200)
                .body("diseases", notNullValue())
                .body("diseases.size()", greaterThan(0));
    }

    @Test
    void alertsShouldReturn200ForDoctor() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .when()
                .get("/doctor/dashboard/alerts")
                .then()
                .statusCode(200)
                .body("alerts", notNullValue());
    }

    @Test
    void localDiseaseBreakdownShouldReturn200ForDoctor() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .when()
                .get("/doctor/dashboard/disease-breakdown/local")
                .then()
                .statusCode(200)
                .body("diseaseBreakdown", notNullValue());
    }

    @Test
    void stateDiseaseBreakdownShouldReturn200ForDoctor() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .when()
                .get("/doctor/dashboard/disease-breakdown/state")
                .then()
                .statusCode(200)
                .body("diseaseBreakdown", notNullValue());
    }

    @Test
    void reportShouldReturn200ForDoctorWithLocalScope() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .when()
                .get("/doctor/dashboard/reports/{scope}", "local")
                .then()
                .statusCode(200);
    }

    @Test
    void stateReportShouldReturn200ForDoctor() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", DOCTOR_EMAIL)
                .when()
                .get("/doctor/dashboard/reports/states/{stateId}", stateId)
                .then()
                .statusCode(200);
    }
}
