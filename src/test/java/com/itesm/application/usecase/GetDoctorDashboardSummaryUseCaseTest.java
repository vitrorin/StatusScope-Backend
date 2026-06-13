package com.itesm.application.usecase;

import com.itesm.application.dto.DoctorDashboardSummaryDto;
import com.itesm.application.dto.HospitalGeoContextDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.Disease;
import com.itesm.domain.models.Hospital;
import com.itesm.domain.models.Municipality;
import com.itesm.domain.models.Outbreak;
import com.itesm.domain.models.State;
import com.itesm.domain.repository.HospitalRepository;
import com.itesm.domain.repository.MunicipalityRepository;
import com.itesm.domain.repository.OutbreakRepository;
import org.junit.jupiter.api.Assertions;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

class GetDoctorDashboardSummaryUseCaseTest {

    private static final UUID HOSPITAL_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID DOCTOR_ID = UUID.fromString("11000000-0000-0000-0000-000000000001");
    private static final UUID STATE_ID = UUID.fromString("40000000-0000-0000-0000-000000000019");
    private static final UUID MUNICIPALITY_ID = UUID.fromString("42000000-0000-0000-0000-000000001003");

    private GetDoctorDashboardSummaryUseCase useCase;
    private AuthenticatedUserContext authenticatedUserContext;
    private HospitalRepository hospitalRepository;
    private OutbreakRepository outbreakRepository;
    private MunicipalityRepository municipalityRepository;
    private HospitalGeoContextService hospitalGeoContextService;

    private Hospital hospital;

    @BeforeEach
    void setUp() {
        authenticatedUserContext = Mockito.mock(AuthenticatedUserContext.class);
        hospitalRepository = Mockito.mock(HospitalRepository.class);
        outbreakRepository = Mockito.mock(OutbreakRepository.class);
        municipalityRepository = Mockito.mock(MunicipalityRepository.class);
        hospitalGeoContextService = Mockito.mock(HospitalGeoContextService.class);

        useCase = new GetDoctorDashboardSummaryUseCase();
        useCase.authenticatedUserContext = authenticatedUserContext;
        useCase.hospitalRepository = hospitalRepository;
        useCase.outbreakRepository = outbreakRepository;
        useCase.municipalityRepository = municipalityRepository;
        useCase.hospitalGeoContextService = hospitalGeoContextService;

        CurrentUser currentUser = new CurrentUser(
                DOCTOR_ID, "ext-id", "doctor@test.local", "Dr. Test",
                HOSPITAL_ID, Set.of("DOCTOR"), Set.of("outbreaks.read"));
        Mockito.when(authenticatedUserContext.getCurrentUser()).thenReturn(currentUser);

        hospital = new Hospital();
        hospital.setId(HOSPITAL_ID);
        hospital.setName("Test Hospital");
        hospital.setMunicipalityId(MUNICIPALITY_ID);
        hospital.setStateId(STATE_ID);
        hospital.setMunicipalityName("Monterrey");
        hospital.setStateName("Nuevo León");
        hospital.setLatitude(new BigDecimal("25.6866"));
        hospital.setLongitude(new BigDecimal("-100.3161"));
        Mockito.when(hospitalRepository.findHospitalById(HOSPITAL_ID)).thenReturn(Optional.of(hospital));

        HospitalGeoContextDto geoContext = new HospitalGeoContextDto();
        geoContext.setIncludedMunicipalityIds(List.of(MUNICIPALITY_ID));
        geoContext.setStateId(STATE_ID);
        geoContext.setRadiusKm(50.0);
        Mockito.when(hospitalGeoContextService.resolve(hospital)).thenReturn(geoContext);
        Mockito.when(hospitalGeoContextService.resolve(Mockito.eq(hospital), Mockito.anyDouble()))
                .thenReturn(geoContext);

        Mockito.when(outbreakRepository.findActiveByMunicipalityIds(Mockito.anyList()))
                .thenReturn(List.of());
        Mockito.when(outbreakRepository.findActiveByMunicipalityIdsOrStateId(Mockito.anyList(), Mockito.any()))
                .thenReturn(List.of());
        Mockito.when(municipalityRepository.listAllDomain()).thenReturn(List.of());
        Mockito.when(outbreakRepository.findAllActiveMunicipal()).thenReturn(List.of());
    }

    // ── No hospital guard ─────────────────────────────────────────────────────

    @Test
    void shouldThrowNotFoundWhenDoctorHasNoHospital() {
        CurrentUser userWithoutHospital = new CurrentUser(
                DOCTOR_ID, "ext-id", "doctor@test.local", "Dr. Test",
                null, Set.of("DOCTOR"), Set.of("outbreaks.read"));
        Mockito.when(authenticatedUserContext.getCurrentUser()).thenReturn(userWithoutHospital);

        Assertions.assertThrows(NotFoundException.class, () -> useCase.execute());
    }

    @Test
    void shouldThrowNotFoundWhenHospitalNotInRepository() {
        Mockito.when(hospitalRepository.findHospitalById(HOSPITAL_ID)).thenReturn(Optional.empty());

        Assertions.assertThrows(NotFoundException.class, () -> useCase.execute());
    }

    // ── Summary fields ────────────────────────────────────────────────────────

    @Test
    void shouldReturnHospitalNameInSummary() {
        DoctorDashboardSummaryDto result = useCase.execute();
        Assertions.assertEquals("Test Hospital", result.getHospitalName());
    }

    @Test
    void shouldReturnMunicipalityNameInSummary() {
        DoctorDashboardSummaryDto result = useCase.execute();
        Assertions.assertEquals("Monterrey", result.getMunicipalityName());
    }

    @Test
    void shouldReturnStateNameInSummary() {
        DoctorDashboardSummaryDto result = useCase.execute();
        Assertions.assertEquals("Nuevo León", result.getStateName());
    }

    @Test
    void shouldReturnRadiusKmInSummary() {
        DoctorDashboardSummaryDto result = useCase.execute();
        Assertions.assertEquals(50.0, result.getRadiusKm(), 0.001);
    }

    @Test
    void shouldReturnGeneratedAtNotNull() {
        DoctorDashboardSummaryDto result = useCase.execute();
        Assertions.assertNotNull(result.getGeneratedAt());
    }

    // ── Disease breakdown with outbreaks ──────────────────────────────────────

    @Test
    void shouldReturnEmptyBreakdownWhenNoOutbreaks() {
        DoctorDashboardSummaryDto result = useCase.execute();
        Assertions.assertNotNull(result.getDiseaseBreakdown());
        Assertions.assertTrue(result.getDiseaseBreakdown().isEmpty());
    }

    @Test
    void shouldAggregateOutbreaksByDisease() {
        Outbreak o1 = makeOutbreak("Dengue", 10, MUNICIPALITY_ID);
        Outbreak o2 = makeOutbreak("Dengue", 20, MUNICIPALITY_ID);
        Outbreak o3 = makeOutbreak("Measles", 5, MUNICIPALITY_ID);

        Mockito.when(outbreakRepository.findActiveByMunicipalityIds(List.of(MUNICIPALITY_ID)))
                .thenReturn(List.of(o1, o2, o3));

        DoctorDashboardSummaryDto result = useCase.execute();

        // Should have 2 distinct disease entries
        Assertions.assertEquals(2, result.getDiseaseBreakdown().size());

        // Dengue should come first (most cases)
        DoctorDashboardSummaryDto.DoctorDashboardDiseaseDto top = result.getDiseaseBreakdown().get(0);
        Assertions.assertEquals("Dengue", top.getDiseaseName());
        Assertions.assertEquals(30, top.getCaseCount());
        Assertions.assertEquals(2, top.getOutbreakCount());
    }

    @Test
    void shouldSortDiseaseBreakdownByCaseCountDescending() {
        Outbreak o1 = makeOutbreak("Measles", 5, MUNICIPALITY_ID);
        Outbreak o2 = makeOutbreak("Dengue", 100, MUNICIPALITY_ID);
        Outbreak o3 = makeOutbreak("Influenza", 50, MUNICIPALITY_ID);

        Mockito.when(outbreakRepository.findActiveByMunicipalityIds(List.of(MUNICIPALITY_ID)))
                .thenReturn(List.of(o1, o2, o3));

        DoctorDashboardSummaryDto result = useCase.execute();

        List<DoctorDashboardSummaryDto.DoctorDashboardDiseaseDto> breakdown = result.getDiseaseBreakdown();
        Assertions.assertEquals("Dengue", breakdown.get(0).getDiseaseName());
        Assertions.assertEquals("Influenza", breakdown.get(1).getDiseaseName());
        Assertions.assertEquals("Measles", breakdown.get(2).getDiseaseName());
    }

    // ── Alerts ────────────────────────────────────────────────────────────────

    @Test
    void shouldReturnNotNullAlerts() {
        DoctorDashboardSummaryDto result = useCase.execute();
        Assertions.assertNotNull(result.getAlerts());
    }

    @Test
    void shouldReturnNotNullZones() {
        DoctorDashboardSummaryDto result = useCase.execute();
        Assertions.assertNotNull(result.getZones());
    }

    // ── Metrics ───────────────────────────────────────────────────────────────

    @Test
    void shouldReturnNotNullMetrics() {
        DoctorDashboardSummaryDto result = useCase.execute();
        Assertions.assertNotNull(result.getMetrics());
    }

    // ── Radius override ───────────────────────────────────────────────────────

    @Test
    void shouldUseRadiusOverrideWhenProvided() {
        useCase.execute(75.0);
        Mockito.verify(hospitalGeoContextService).resolve(hospital, 75.0);
    }

    @Test
    void shouldUseDefaultGeoContextWhenNoRadiusOverride() {
        useCase.execute(null);
        Mockito.verify(hospitalGeoContextService).resolve(hospital);
    }

    // ── State breakdown ───────────────────────────────────────────────────────

    @Test
    void shouldPopulateStateDiseaseBreakdownFromStateOutbreaks() {
        Outbreak stateOutbreak = makeOutbreak("Tuberculosis", 8, null);
        Mockito.when(outbreakRepository.findActiveByMunicipalityIdsOrStateId(List.of(), STATE_ID))
                .thenReturn(List.of(stateOutbreak));

        DoctorDashboardSummaryDto result = useCase.execute();
        Assertions.assertNotNull(result.getStateDiseaseBreakdown());
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private Outbreak makeOutbreak(String diseaseName, int caseCount, UUID municipalityId) {
        Disease disease = new Disease();
        disease.setId(UUID.randomUUID());
        disease.setName(diseaseName);
        disease.setSymptoms("Generic symptoms");

        Municipality municipality = null;
        if (municipalityId != null) {
            municipality = new Municipality();
            municipality.setId(municipalityId);
            municipality.setName("Monterrey");
            municipality.setStateId(STATE_ID);
            municipality.setStateName("Nuevo León");
        }

        State state = new State();
        state.setId(STATE_ID);
        state.setName("Nuevo León");

        Outbreak o = new Outbreak();
        o.setId(UUID.randomUUID());
        o.setDisease(disease);
        o.setScope(municipalityId != null ? "MUNICIPALITY" : "STATE");
        o.setMunicipality(municipality);
        o.setState(state);
        o.setCaseCount(caseCount);
        o.setConfirmationStatus("CONFIRMED");
        o.setStartedAt(LocalDateTime.now().minusDays(5));
        o.setStatus("ACTIVE");
        return o;
    }
}
