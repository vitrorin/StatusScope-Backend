package com.itesm.application.usecase;

import com.itesm.application.dto.DoctorDashboardSummaryDto;
import com.itesm.application.dto.DoctorDashboardSummaryDto.DoctorDashboardAlertDto;
import com.itesm.application.dto.DoctorDashboardSummaryDto.DoctorDashboardDiseaseDto;
import com.itesm.application.dto.DoctorDashboardSummaryDto.DoctorDashboardMetricDto;
import com.itesm.application.dto.DoctorDashboardSummaryDto.DoctorDashboardZoneDto;
import com.itesm.application.dto.HospitalGeoContextDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.Hospital;
import com.itesm.domain.models.Outbreak;
import com.itesm.domain.repository.HospitalRepository;
import com.itesm.domain.repository.OutbreakRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class GetDoctorDashboardSummaryUseCase {

    @Inject
    AuthenticatedUserContext authenticatedUserContext;

    @Inject
    HospitalRepository hospitalRepository;

    @Inject
    OutbreakRepository outbreakRepository;

    @Inject
    HospitalGeoContextService hospitalGeoContextService;

    public DoctorDashboardSummaryDto execute() {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        UUID hospitalId = currentUser.getHospitalId();
        if (hospitalId == null) {
            throw new NotFoundException("Doctor has no assigned hospital");
        }

        Hospital hospital = hospitalRepository.findHospitalById(hospitalId)
                .orElseThrow(() -> new NotFoundException("Hospital not found for id: " + hospitalId));

        HospitalGeoContextDto geoContext = hospitalGeoContextService.resolve(hospital);
        List<Outbreak> outbreaks = outbreakRepository.findActiveByMunicipalityIds(geoContext.getIncludedMunicipalityIds());
        List<Outbreak> stateOutbreaks = outbreakRepository.findActiveByMunicipalityIdsOrStateId(
                List.of(),
                geoContext.getStateId());

        List<DoctorDashboardDiseaseDto> diseaseBreakdown = buildDiseaseBreakdown(outbreaks);
        List<DoctorDashboardDiseaseDto> stateDiseaseBreakdown = buildDiseaseBreakdown(stateOutbreaks);
        int totalCases = outbreaks.stream().mapToInt(Outbreak::getCaseCount).sum();
        DoctorDashboardDiseaseDto topDisease = diseaseBreakdown.isEmpty() ? null : diseaseBreakdown.get(0);

        DoctorDashboardSummaryDto summary = new DoctorDashboardSummaryDto();
        summary.setHospitalName(hospital.getName());
        summary.setMunicipalityName(hospital.getMunicipalityName());
        summary.setStateName(hospital.getStateName());
        summary.setRadiusKm(geoContext.getRadiusKm());
        summary.setGeneratedAt(LocalDateTime.now());
        summary.setMetrics(buildMetrics(hospital, totalCases, outbreaks.size(), topDisease, geoContext.getRadiusKm()));
        summary.setDiseaseBreakdown(diseaseBreakdown);
        summary.setStateDiseaseBreakdown(stateDiseaseBreakdown);
        summary.setAlerts(buildAlerts(outbreaks));
        summary.setZones(buildZones(outbreaks, geoContext));
        return summary;
    }

    private List<DoctorDashboardMetricDto> buildMetrics(
            Hospital hospital,
            int totalCases,
            int outbreakCount,
            DoctorDashboardDiseaseDto topDisease,
            double radiusKm
    ) {
        String roundedRadius = String.valueOf(Math.round(radiusKm));
        String topDiseaseName = topDisease == null ? "No active outbreaks" : topDisease.getDiseaseName();
        String topDiseaseCases = topDisease == null ? "0 cases" : formatCount(topDisease.getCaseCount()) + " cases";
        String riskLabel = riskLabel(outbreakCount, totalCases);
        String capacityValue = hospital.getBedCount() == null ? "Not configured" : formatCount(hospital.getBedCount()) + " beds";
        String staffingBadge = hospital.getDoctorCount() == null && hospital.getNurseCount() == null
                ? "Staff pending"
                : "%s MD / %s RN".formatted(
                        hospital.getDoctorCount() == null ? "0" : formatCount(hospital.getDoctorCount()),
                        hospital.getNurseCount() == null ? "0" : formatCount(hospital.getNurseCount()));

        return List.of(
                new DoctorDashboardMetricDto(
                        "active-cases-nearby",
                        "Active Cases Nearby",
                        formatCount(totalCases),
                        roundedRadius + " km",
                        totalCases > 0 ? "warning" : "positive",
                        "Active outbreak cases in hospital context",
                        "Sum of active municipal outbreaks in the hospital radius plus active state-level outbreaks for the hospital state.",
                        totalCases > 0 ? "Active regional load" : "No active regional load",
                        "Keep triage aligned with diseases currently active near the hospital.",
                        null),
                new DoctorDashboardMetricDto(
                        "highest-case-disease",
                        "Highest Case Disease",
                        topDiseaseName,
                        topDiseaseCases,
                        topDisease == null ? "neutral" : "danger",
                        "Largest active case count in current context",
                        "This replaces projected growth until weekly historical series are available.",
                        topDisease == null ? "No disease signal" : "Largest current burden",
                        "Use this signal as epidemiological context, not as a standalone diagnosis.",
                        "trend"),
                new DoctorDashboardMetricDto(
                        "local-risk-level",
                        "Local Risk Level",
                        outbreakCount + " active outbreaks",
                        riskLabel,
                        riskStatus(riskLabel),
                        "Based on active outbreak count and total cases",
                        "Risk is derived from active outbreak volume in the hospital geographic context.",
                        riskLabel + " pressure",
                        "Review nearby outbreaks before evaluating compatible symptoms.",
                        null),
                new DoctorDashboardMetricDto(
                        "hospital-profile",
                        "Hospital Profile",
                        capacityValue,
                        staffingBadge,
                        "neutral",
                        "Registered facility capacity",
                        "This is configured hospital capacity, not live bed occupancy.",
                        "Facility baseline",
                        "Connect bed occupancy and staffing shifts later for real operational capacity.",
                        null)
        );
    }

    private List<DoctorDashboardDiseaseDto> buildDiseaseBreakdown(List<Outbreak> outbreaks) {
        Map<String, DiseaseAggregate> aggregates = new LinkedHashMap<>();
        for (Outbreak outbreak : outbreaks) {
            if (outbreak.getDisease() == null || outbreak.getDisease().getName() == null) continue;
            aggregates.computeIfAbsent(outbreak.getDisease().getName(), DiseaseAggregate::new).add(outbreak);
        }

        int maxCases = aggregates.values().stream()
                .mapToInt(DiseaseAggregate::caseCount)
                .max()
                .orElse(0);

        return aggregates.values().stream()
                .sorted(Comparator.comparingInt(DiseaseAggregate::caseCount).reversed())
                .limit(5)
                .map(aggregate -> new DoctorDashboardDiseaseDto(
                        aggregate.diseaseName(),
                        aggregate.caseCount(),
                        aggregate.outbreakCount(),
                        maxCases == 0 ? 0 : Math.max(6, Math.round((aggregate.caseCount() * 100f) / maxCases))))
                .collect(Collectors.toList());
    }

    private List<DoctorDashboardAlertDto> buildAlerts(List<Outbreak> outbreaks) {
        return outbreaks.stream()
                .filter(outbreak -> outbreak.getDisease() != null)
                .sorted(Comparator.comparingInt(Outbreak::getCaseCount).reversed())
                .limit(4)
                .map(outbreak -> {
                    String disease = outbreak.getDisease().getName();
                    String location = locationLabel(outbreak);
                    String variant = alertVariant(outbreak.getCaseCount(), outbreak.getConfirmationStatus());
                    return new DoctorDashboardAlertDto(
                            outbreak.getId().toString(),
                            disease + " activity",
                            "%s active %s in %s. Status: %s.".formatted(
                                    formatCount(outbreak.getCaseCount()),
                                    outbreak.getCaseCount() == 1 ? "case" : "cases",
                                    location,
                                    outbreak.getConfirmationStatus()),
                            variant,
                            location,
                            alertPriority(variant),
                            "Compare compatible patient symptoms against this active outbreak before closing the evaluation.");
                })
                .collect(Collectors.toList());
    }

    private List<DoctorDashboardZoneDto> buildZones(List<Outbreak> outbreaks, HospitalGeoContextDto geoContext) {
        List<DoctorDashboardZoneDto> zones = new ArrayList<>();
        if (geoContext.getLatitude() != null && geoContext.getLongitude() != null) {
            zones.add(new DoctorDashboardZoneDto(
                    "hospital-node",
                    geoContext.getMunicipalityName() == null ? "Hospital node" : geoContext.getMunicipalityName() + " hospital node",
                    "Monitored",
                    "Hospital context",
                    "Current facility",
                    "0 km",
                    "Operational review",
                    "Reference point for nearby outbreak context.",
                    "Use this point as the center of the doctor dashboard surveillance radius.",
                    geoContext.getLatitude(),
                    geoContext.getLongitude(),
                    "#0003B8"));
        }

        outbreaks.stream()
                .filter(outbreak -> outbreak.getDisease() != null)
                .filter(outbreak -> outbreak.getMunicipality() != null)
                .filter(outbreak -> outbreak.getMunicipality().getLatitude() != null && outbreak.getMunicipality().getLongitude() != null)
                .sorted(Comparator.comparingInt(Outbreak::getCaseCount).reversed())
                .limit(5)
                .forEach(outbreak -> zones.add(new DoctorDashboardZoneDto(
                        outbreak.getId().toString(),
                        outbreak.getMunicipality().getName(),
                        riskLabel(1, outbreak.getCaseCount()),
                        outbreak.getDisease().getName(),
                        formatCount(outbreak.getCaseCount()) + " active cases",
                        "Within " + Math.round(geoContext.getRadiusKm()) + " km",
                        alertPriority(alertVariant(outbreak.getCaseCount(), outbreak.getConfirmationStatus())),
                        "Municipal outbreak signal in the hospital context radius.",
                        "Keep this disease in the differential when symptoms overlap.",
                        outbreak.getMunicipality().getLatitude(),
                        outbreak.getMunicipality().getLongitude(),
                        colorForCaseCount(outbreak.getCaseCount()))));
        return zones;
    }

    private String riskLabel(int outbreakCount, int totalCases) {
        if (outbreakCount >= 10 || totalCases >= 500) return "High";
        if (outbreakCount >= 4 || totalCases >= 100) return "Moderate";
        if (outbreakCount > 0) return "Low";
        return "Clear";
    }

    private String riskStatus(String riskLabel) {
        return switch (riskLabel) {
            case "High" -> "danger";
            case "Moderate" -> "warning";
            case "Low" -> "neutral";
            default -> "positive";
        };
    }

    private String alertVariant(int caseCount, String confirmationStatus) {
        if (caseCount >= 100 && "CONFIRMED".equals(confirmationStatus)) return "critical";
        if (caseCount >= 25) return "warning";
        if ("CONFIRMED".equals(confirmationStatus)) return "info";
        return "neutral";
    }

    private String alertPriority(String variant) {
        return switch (variant) {
            case "critical" -> "Immediate";
            case "warning" -> "High";
            case "info" -> "Review";
            default -> "Routine";
        };
    }

    private String colorForCaseCount(int caseCount) {
        if (caseCount >= 100) return "#EF4444";
        if (caseCount >= 25) return "#F97316";
        return "#FB923C";
    }

    private String locationLabel(Outbreak outbreak) {
        if (outbreak.getMunicipality() != null) {
            return outbreak.getMunicipality().getName();
        }
        if (outbreak.getState() != null) {
            return outbreak.getState().getName();
        }
        return "hospital region";
    }

    private String formatCount(int value) {
        return String.format("%,d", value);
    }

    private static class DiseaseAggregate {
        private final String diseaseName;
        private int caseCount;
        private int outbreakCount;

        DiseaseAggregate(String diseaseName) {
            this.diseaseName = diseaseName;
        }

        void add(Outbreak outbreak) {
            caseCount += outbreak.getCaseCount();
            outbreakCount++;
        }

        String diseaseName() { return diseaseName; }
        int caseCount() { return caseCount; }
        int outbreakCount() { return outbreakCount; }
    }
}
