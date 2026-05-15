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
import static org.hamcrest.Matchers.hasItem;

@QuarkusTest
class DoctorDashboardResourceTest {

    private static final String DOCTOR_EMAIL = "dashboard-doctor@statusscope.local";
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
}
