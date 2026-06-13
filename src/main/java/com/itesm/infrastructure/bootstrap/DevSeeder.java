package com.itesm.infrastructure.bootstrap;

import com.google.firebase.auth.FirebaseAuthException;
import com.itesm.domain.models.Role;
import com.itesm.domain.models.User;
import com.itesm.domain.models.UserStatus;
import com.itesm.domain.repository.HospitalRepository;
import com.itesm.domain.repository.RoleRepository;
import com.itesm.domain.repository.UserRepository;
import com.itesm.infrastructure.firebase.FirebaseConfig;
import com.itesm.infrastructure.firebase.FirebaseUserService;
import com.itesm.infrastructure.persistence.entity.DiseaseEntity;
import com.itesm.infrastructure.persistence.entity.EvaluationDifferentialDiagnosisEntity;
import com.itesm.infrastructure.persistence.entity.EvaluationRecommendedTestEntity;
import com.itesm.infrastructure.persistence.entity.EventEntity;
import com.itesm.infrastructure.persistence.entity.HospitalEntity;
import com.itesm.infrastructure.persistence.entity.MunicipalityEntity;
import com.itesm.infrastructure.persistence.entity.OutbreakEntity;
import com.itesm.infrastructure.persistence.entity.PatientEntity;
import com.itesm.infrastructure.persistence.entity.PatientEvaluationEntity;
import com.itesm.infrastructure.persistence.entity.PatientEvaluationFileEntity;
import com.itesm.infrastructure.persistence.entity.UserEntity;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class DevSeeder {

    private static final Logger LOG = Logger.getLogger(DevSeeder.class);

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    HospitalRepository hospitalRepository;

    @Inject
    FirebaseUserService firebaseUserService;

    @Inject
    Instance<FirebaseConfig> firebaseConfig;

    @Inject
    EntityManager entityManager;

    @ConfigProperty(name = "quarkus.profile")
    String profile;

    @ConfigProperty(name = "statusscope.dev-seeder.enabled", defaultValue = "false")
    boolean devSeederEnabled;

    @ConfigProperty(name = "statusscope.dev-seeder.seed-password")
    Optional<String> devSeedPassword;

    @ConfigProperty(name = "diagnosis.assistant.seed-demo-data", defaultValue = "false")
    boolean seedDiagnosisAssistantDemoData;

    @Transactional
    void onStart(@Observes @Priority(20) StartupEvent ev) {
        if (!devSeederEnabled && !"dev".equals(profile) && !"test".equals(profile)) return;
        if (devSeederEnabled || "dev".equals(profile)) {
            ensureFirebaseInitialized();
        }
        if (userRepository.findByEmail("admin@statusscope.local").isPresent()) return; // idempotent

        seedUser("admin@statusscope.local",       "System Admin",    null,
                "SYSTEM_ADMIN", null);
        seedUser("admin.hgz21@statusscope.local", "Admin HGZ-21",
                UUID.fromString("30000000-0000-0000-0000-000000000001"),
                "HOSPITAL_ADMIN", null);
        seedUser("admin.hre05@statusscope.local", "Admin HRE-05",
                UUID.fromString("30000000-0000-0000-0000-000000000002"),
                "HOSPITAL_ADMIN", 1);
        seedUser("doctor1@statusscope.local",     "Dra. Ana López",
                UUID.fromString("30000000-0000-0000-0000-000000000001"),
                "DOCTOR", null);
        seedUser("doctor2@statusscope.local",     "Dr. Luis Pérez",
                UUID.fromString("30000000-0000-0000-0000-000000000002"),
                "DOCTOR", 0);
        if (seedDiagnosisAssistantDemoData) {
            seedDiagnosisAssistantFixtures();
        }
    }

    private void ensureFirebaseInitialized() {
        if (firebaseConfig.isResolvable()) {
            firebaseConfig.get().ensureInitialized();
        }
    }

    private void seedUser(String email, String fullName, UUID hospitalId, String roleCode, Integer lastLoginDaysAgo) {
        String uid;
        try {
            uid = firebaseUserService.createUser(email, devSeedPassword.orElseThrow(
                    () -> new IllegalStateException("DEV_SEEDER_SEED_PASSWORD is required for dev seeding")), fullName);
        } catch (FirebaseAuthException e) {
            // Idempotent restart: Firebase user might already exist
            try {
                var record = firebaseUserService.getUserByEmail(email);
                if (record != null) {
                    uid = record.getUid();
                } else {
                    LOG.errorf("DevSeeder: could not create or find Firebase user for %s: %s", email, e.getMessage());
                    return;
                }
            } catch (FirebaseAuthException ex) {
                LOG.errorf("DevSeeder: error looking up Firebase user %s: %s", email, ex.getMessage());
                return;
            }
        }

        Role role = roleRepository.findByCode(roleCode).orElse(null);
        if (role == null) {
            LOG.errorf("DevSeeder: role %s was not found; skipping user %s", roleCode, email);
            return;
        }

        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail(email);
        u.setFullName(fullName);
        u.setHospitalId(hospitalId);
        u.setExternalAuthId(uid);
        u.setStatus(UserStatus.ACTIVE);
        u.setActive(true);
        if (lastLoginDaysAgo != null) {
            u.setLastLoginAt(LocalDateTime.now().minusDays(lastLoginDaysAgo).minusHours(lastLoginDaysAgo % 3));
        }
        u.setRoles(Set.of(role));
        userRepository.create(u);
        LOG.infof("DevSeeder: seeded user %s", email);
    }

    private void seedDiagnosisAssistantFixtures() {
        UUID hospitalId = UUID.fromString("30000000-0000-0000-0000-000000000001");
        UUID municipalityId = UUID.fromString("42000000-0000-0000-0000-000000001003");
        UUID covidId = UUID.fromString("60000000-0000-0000-0000-000000000004");
        UUID patientId = UUID.fromString("71000000-0000-0000-0000-000000000001");
        UUID evaluationId = UUID.fromString("71000000-0000-0000-0000-000000000101");
        UUID fileId = UUID.fromString("71000000-0000-0000-0000-000000000201");
        UUID testId = UUID.fromString("71000000-0000-0000-0000-000000000301");
        UUID differentialId = UUID.fromString("71000000-0000-0000-0000-000000000401");
        UUID outbreakId = UUID.fromString("71000000-0000-0000-0000-000000000501");
        UUID eventId = UUID.fromString("71000000-0000-0000-0000-000000000601");

        User doctor = userRepository.findByEmail("doctor1@statusscope.local").orElse(null);
        if (doctor == null) {
            LOG.warn("DevSeeder: doctor1 user was not found; skipping diagnosis assistant fixtures");
            return;
        }

        HospitalEntity hospital = entityManager.getReference(HospitalEntity.class, hospitalId);
        MunicipalityEntity municipality = entityManager.getReference(MunicipalityEntity.class, municipalityId);
        DiseaseEntity covid = entityManager.getReference(DiseaseEntity.class, covidId);
        UserEntity doctorEntity = entityManager.getReference(UserEntity.class, doctor.getId());

        PatientEntity patient = entityManager.find(PatientEntity.class, patientId);
        if (patient == null) {
            patient = new PatientEntity();
            patient.setId(patientId);
            patient.setHospital(hospital);
            patient.setFullName("Sofia Martinez");
            patient.setSex("female");
            patient.setBirthDate(LocalDate.now().minusYears(7));
            patient.setWeightKg(new BigDecimal("24.50"));
            patient.setHeightCm(new BigDecimal("121.00"));
            patient.setPostalCode("64000");
            patient.setCreatedAt(LocalDateTime.now());
            patient.setUpdatedAt(LocalDateTime.now());
            entityManager.persist(patient);
        }

        if (entityManager.find(OutbreakEntity.class, outbreakId) == null) {
            OutbreakEntity outbreak = new OutbreakEntity();
            outbreak.setId(outbreakId);
            outbreak.setDisease(covid);
            outbreak.setScope("MUNICIPALITY");
            outbreak.setMunicipality(municipality);
            outbreak.setCaseCount(12);
            outbreak.setConfirmationStatus("CONFIRMED");
            outbreak.setStatus("ACTIVE");
            outbreak.setStartedAt(LocalDateTime.now().minusDays(2));
            outbreak.setCreatedAt(LocalDateTime.now());
            outbreak.setUpdatedAt(LocalDateTime.now());
            entityManager.persist(outbreak);
        }

        EventEntity event = entityManager.find(EventEntity.class, eventId);
        if (event == null) {
            event = new EventEntity();
            event.setId(eventId);
            event.setPatient(patient);
            event.setDisease(covid);
            event.setPrimaryDoctor(doctorEntity);
            event.setStatus("ACTIVE");
            event.setStartedAt(LocalDateTime.now().minusHours(4));
            event.setCreatedAt(LocalDateTime.now());
            event.setUpdatedAt(LocalDateTime.now());
            entityManager.persist(event);
        }

        PatientEvaluationEntity evaluation = entityManager.find(PatientEvaluationEntity.class, evaluationId);
        if (evaluation == null) {
            evaluation = new PatientEvaluationEntity();
            evaluation.setId(evaluationId);
            evaluation.setPatient(patient);
            evaluation.setDoctor(doctorEntity);
            evaluation.setEvent(event);
            evaluation.setStatus("IN_PROGRESS");
            evaluation.setSymptomsText("High fever, conjunctivitis, watery eyes, and rash starting on the face.");
            evaluation.setClinicalNotes("Seeded dev evaluation for diagnosis assistant checks.");
            evaluation.setCreatedAt(LocalDateTime.now());
            evaluation.setUpdatedAt(LocalDateTime.now());
            entityManager.persist(evaluation);
        }

        if (entityManager.find(PatientEvaluationFileEntity.class, fileId) == null) {
            PatientEvaluationFileEntity file = new PatientEvaluationFileEntity();
            file.setId(fileId);
            file.setEvaluation(evaluation);
            file.setFileName("seeded_lab_panel.pdf");
            file.setMimeType("application/pdf");
            file.setStorageKey("dev-seed/seeded_lab_panel.pdf");
            file.setFileSizeBytes(182_044L);
            file.setDocumentType("LAB_RESULT");
            file.setUploadedAt(LocalDateTime.now());
            entityManager.persist(file);
        }

        if (entityManager.find(EvaluationRecommendedTestEntity.class, testId) == null) {
            EvaluationRecommendedTestEntity test = new EvaluationRecommendedTestEntity();
            test.setId(testId);
            test.setEvaluation(evaluation);
            test.setTestName("SARS-CoV-2 PCR");
            test.setReason("Respiratory and febrile presentation overlaps with the active regional outbreak.");
            test.setSource("AI");
            test.setSortOrder(0);
            test.setCreatedAt(LocalDateTime.now());
            entityManager.persist(test);
        }

        if (entityManager.find(EvaluationDifferentialDiagnosisEntity.class, differentialId) == null) {
            EvaluationDifferentialDiagnosisEntity differential = new EvaluationDifferentialDiagnosisEntity();
            differential.setId(differentialId);
            differential.setEvaluation(evaluation);
            differential.setDisease(covid);
            differential.setDisplayName("COVID-19");
            differential.setConfidence(new BigDecimal("84.50"));
            differential.setRationale("Symptoms overlap with the active outbreak seeded for Hospital General Zona 21.");
            differential.setRankOrder(0);
            differential.setLocalityRiskLevel("HIGH");
            differential.setCreatedAt(LocalDateTime.now());
            entityManager.persist(differential);
        }

        LOG.info("DevSeeder: seeded diagnosis assistant fixtures");
    }
}
