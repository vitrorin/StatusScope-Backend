package com.itesm.application.usecase;

import com.itesm.application.dto.DoctorDashboardSummaryDto;
import com.itesm.application.dto.DoctorDashboardSummaryDto.DoctorDashboardAlertDto;
import com.itesm.application.dto.DoctorDashboardSummaryDto.DoctorDashboardDiseaseDto;
import com.itesm.application.dto.DoctorDashboardSummaryDto.DoctorDashboardMetricInsightDto;
import com.itesm.application.dto.DoctorDashboardSummaryDto.DoctorDashboardMetricDto;
import com.itesm.application.dto.DoctorDashboardSummaryDto.DoctorDashboardZoneDto;
import com.itesm.application.dto.HospitalGeoContextDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.Hospital;
import com.itesm.domain.models.Municipality;
import com.itesm.domain.models.Outbreak;
import com.itesm.domain.repository.HospitalRepository;
import com.itesm.domain.repository.MunicipalityRepository;
import com.itesm.domain.repository.OutbreakRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class GetDoctorDashboardSummaryUseCase {
    private static final Set<String> PRIORITY_DISEASES = Set.of(
            "covid 19",
            "dengue fever",
            "dengue severe",
            "hiv aids",
            "influenza",
            "malaria",
            "measles",
            "meningitis",
            "mpox",
            "tuberculosis",
            "whooping cough"
    );

    @Inject
    AuthenticatedUserContext authenticatedUserContext;

    @Inject
    HospitalRepository hospitalRepository;

    @Inject
    OutbreakRepository outbreakRepository;

    @Inject
    MunicipalityRepository municipalityRepository;

    @Inject
    HospitalGeoContextService hospitalGeoContextService;

    public DoctorDashboardSummaryDto execute() {
        return execute(null);
    }

    public DoctorDashboardSummaryDto execute(Double radiusKmOverride) {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        UUID hospitalId = currentUser.getHospitalId();
        if (hospitalId == null) {
            throw new NotFoundException("Doctor has no assigned hospital");
        }

        Hospital hospital = hospitalRepository.findHospitalById(hospitalId)
                .orElseThrow(() -> new NotFoundException("Hospital not found for id: " + hospitalId));

        HospitalGeoContextDto geoContext = radiusKmOverride == null
                ? hospitalGeoContextService.resolve(hospital)
                : hospitalGeoContextService.resolve(hospital, radiusKmOverride);
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
        summary.setMetrics(buildMetrics(totalCases, outbreaks.size(), topDisease, geoContext, outbreaks));
        summary.setDiseaseBreakdown(diseaseBreakdown);
        summary.setStateDiseaseBreakdown(stateDiseaseBreakdown);
        summary.setAlerts(buildAlerts(outbreaks));
        summary.setZones(buildZones(outbreaks, geoContext, hospital));
        return summary;
    }

    public List<DoctorDashboardStateMapDto> listStateMap() {
        Map<UUID, StateAggregate> states = new LinkedHashMap<>();
        for (Municipality municipality : municipalityRepository.listAllDomain()) {
            if (municipality.getStateId() == null || municipality.getLatitude() == null || municipality.getLongitude() == null) {
                continue;
            }
            states.computeIfAbsent(
                    municipality.getStateId(),
                    id -> new StateAggregate(id, municipality.getStateName()))
                    .addMunicipality(municipality);
        }

        for (Outbreak outbreak : outbreakRepository.findAllActiveMunicipal()) {
            if (outbreak.getMunicipality() == null || outbreak.getMunicipality().getStateId() == null) continue;
            StateAggregate aggregate = states.get(outbreak.getMunicipality().getStateId());
            if (aggregate != null) {
                aggregate.addOutbreak(outbreak);
            }
        }

        return states.values().stream()
                .filter(StateAggregate::hasCoordinates)
                .sorted(Comparator.comparing(StateAggregate::stateName))
                .map(StateAggregate::toDto)
                .toList();
    }

    public DoctorDashboardSummaryDto stateMap(UUID stateId) {
        List<Outbreak> outbreaks = outbreakRepository.findActiveMunicipalByStateId(stateId);
        DoctorDashboardSummaryDto summary = new DoctorDashboardSummaryDto();
        summary.setGeneratedAt(LocalDateTime.now());
        summary.setDiseaseBreakdown(buildDiseaseBreakdown(outbreaks));
        summary.setZones(buildZones(outbreaks, null, null));
        return summary;
    }

    public DoctorDashboardReportDto stateReport(UUID stateId) {
        List<Outbreak> outbreaks = outbreakRepository.findActiveStateByStateId(stateId);
        String stateName = outbreaks.stream()
                .map(this::stateName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(null);

        List<DoctorDashboardReportOutbreakDto> rows = outbreaks.stream()
                .filter(outbreak -> outbreak.getDisease() != null)
                .sorted(Comparator
                        .comparingInt((Outbreak outbreak) -> severityRank(evaluateOutbreakSeverity(outbreak))).reversed()
                        .thenComparing(Comparator.comparingInt(Outbreak::getCaseCount).reversed())
                        .thenComparing(this::locationWithState))
                .map(outbreak -> new DoctorDashboardReportOutbreakDto(
                        outbreak.getId(),
                        outbreak.getDisease().getName(),
                        locationWithState(outbreak),
                        outbreak.getScope(),
                        outbreak.getCaseCount(),
                        outbreak.getConfirmationStatus(),
                        outbreak.getStartedAt()))
                .toList();

        return new DoctorDashboardReportDto(
                "state",
                null,
                null,
                stateName,
                LocalDateTime.now(),
                rows);
    }

    public DoctorDashboardReportDto report(String scope, Double radiusKmOverride) {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        UUID hospitalId = currentUser.getHospitalId();
        if (hospitalId == null) {
            throw new NotFoundException("Doctor has no assigned hospital");
        }

        Hospital hospital = hospitalRepository.findHospitalById(hospitalId)
                .orElseThrow(() -> new NotFoundException("Hospital not found for id: " + hospitalId));

        HospitalGeoContextDto geoContext = radiusKmOverride == null
                ? hospitalGeoContextService.resolve(hospital)
                : hospitalGeoContextService.resolve(hospital, radiusKmOverride);
        List<Outbreak> localOutbreaks = outbreakRepository.findActiveByMunicipalityIds(geoContext.getIncludedMunicipalityIds());
        List<Outbreak> stateOutbreaks = outbreakRepository.findActiveByMunicipalityIdsOrStateId(
                List.of(),
                geoContext.getStateId());
        String normalizedScope = scope == null ? "both" : scope.toLowerCase();
        List<Outbreak> reportOutbreaks = switch (normalizedScope) {
            case "local" -> localOutbreaks;
            case "state" -> stateOutbreaks;
            case "both" -> {
                List<Outbreak> combined = new ArrayList<>(localOutbreaks);
                combined.addAll(stateOutbreaks);
                yield combined;
            }
            default -> throw new IllegalArgumentException("Unsupported report scope: " + scope);
        };

        List<DoctorDashboardReportOutbreakDto> rows = reportOutbreaks.stream()
                .filter(outbreak -> outbreak.getDisease() != null)
                .sorted(Comparator
                        .comparingInt((Outbreak outbreak) -> severityRank(evaluateOutbreakSeverity(outbreak))).reversed()
                        .thenComparing(Comparator.comparingInt(Outbreak::getCaseCount).reversed())
                        .thenComparing(this::locationWithState))
                .map(outbreak -> new DoctorDashboardReportOutbreakDto(
                        outbreak.getId(),
                        outbreak.getDisease().getName(),
                        locationWithState(outbreak),
                        outbreak.getScope(),
                        outbreak.getCaseCount(),
                        outbreak.getConfirmationStatus(),
                        outbreak.getStartedAt()))
                .toList();

        return new DoctorDashboardReportDto(
                normalizedScope,
                hospital.getName(),
                hospital.getMunicipalityName(),
                hospital.getStateName(),
                LocalDateTime.now(),
                rows);
    }

    private List<DoctorDashboardMetricDto> buildMetrics(
            int totalCases,
            int outbreakCount,
            DoctorDashboardDiseaseDto topDisease,
            HospitalGeoContextDto geoContext,
            List<Outbreak> outbreaks
    ) {
        double radiusKm = geoContext.getRadiusKm();
        String roundedRadius = String.valueOf(Math.round(radiusKm));
        String topDiseaseName = topDisease == null ? "No active outbreaks" : topDisease.getDiseaseName();
        String topDiseaseCases = topDisease == null ? "0 cases" : formatCount(topDisease.getCaseCount()) + " cases";
        DashboardSeverity contextSeverity = evaluateContextSeverity(totalCases, outbreakCount);
        DashboardSeverity topDiseaseSeverity = topDisease == null
                ? DashboardSeverity.LOW
                : evaluateAggregateSeverity(topDisease.getDiseaseName(), topDisease.getCaseCount(), topDisease.getOutbreakCount());
        String riskLabel = contextSeverity.label(outbreakCount == 0 && totalCases == 0);
        MunicipalityPriorityAggregate priorityMunicipality = topPriorityMunicipality(outbreaks);

        return List.of(
                new DoctorDashboardMetricDto(
                        "active-cases-nearby",
                        "Active Cases Nearby",
                        formatCount(totalCases) + (totalCases == 1 ? " case" : " cases"),
                        roundedRadius + " km",
                        severityStatus(contextSeverity),
                        "Active outbreak cases in hospital context",
                        "Sum of active municipal outbreaks in the hospital radius plus active state-level outbreaks for the hospital state.",
                        totalCases > 0 ? "Active regional load" : "No active regional load",
                        "Keep triage aligned with diseases currently active near the hospital.",
                        null,
                        topSevereOutbreakInsights(outbreaks)),
                new DoctorDashboardMetricDto(
                        "highest-case-disease",
                        "Highest Case Disease",
                        topDiseaseName,
                        topDiseaseCases,
                        topDisease == null ? "neutral" : severityStatus(topDiseaseSeverity),
                        "Largest active case count in current context",
                        "This replaces projected growth until weekly historical series are available.",
                        topDisease == null ? "No disease signal" : "Largest current burden",
                        "Use this signal as epidemiological context, not as a standalone diagnosis.",
                        "trend",
                        topDiseaseLocationInsights(outbreaks, topDisease == null ? null : topDisease.getDiseaseName())),
                new DoctorDashboardMetricDto(
                        "local-risk-level",
                        "Local Risk Level",
                        outbreakCount + " active outbreaks",
                        riskLabel,
                        severityStatus(contextSeverity),
                        "Based on active outbreak count and total cases",
                        "Risk is derived from active outbreak volume in the hospital geographic context.",
                        riskLabel + " pressure",
                        "Review nearby outbreaks before evaluating compatible symptoms.",
                        null,
                        nearestOutbreakInsights(outbreaks, geoContext)),
                new DoctorDashboardMetricDto(
                        "priority-municipality",
                        "Priority Municipality",
                        priorityMunicipality == null ? "No priority outbreaks" : priorityMunicipality.municipalityName(),
                        priorityMunicipality == null
                                ? "0 priority"
                                : formatCount(priorityMunicipality.outbreakCount()) + " priority",
                        priorityMunicipality == null ? "positive" : severityStatus(priorityMunicipality.highestSeverity()),
                        "Municipality with the most high or moderate outbreak signals",
                        "Groups high and moderate severity municipal outbreaks inside the selected radius.",
                        priorityMunicipality == null ? "No priority municipality" : "Priority focus",
                        "Prioritize this municipality when reviewing compatible symptoms and nearby outbreak context.",
                        null,
                        priorityMunicipalityInsights(outbreaks, priorityMunicipality))
        );
    }

    private List<DoctorDashboardMetricInsightDto> topSevereOutbreakInsights(List<Outbreak> outbreaks) {
        return outbreaks.stream()
                .filter(outbreak -> outbreak.getDisease() != null)
                .sorted(Comparator
                        .comparingInt((Outbreak outbreak) -> severityRank(evaluateOutbreakSeverity(outbreak))).reversed()
                        .thenComparing(Comparator.comparingInt(Outbreak::getCaseCount).reversed()))
                .limit(5)
                .map(outbreak -> insightForOutbreak(outbreak, null))
                .toList();
    }

    private List<DoctorDashboardMetricInsightDto> topDiseaseLocationInsights(List<Outbreak> outbreaks, String diseaseName) {
        if (diseaseName == null) return List.of();
        return outbreaks.stream()
                .filter(outbreak -> outbreak.getDisease() != null)
                .filter(outbreak -> diseaseName.equals(outbreak.getDisease().getName()))
                .sorted(Comparator.comparingInt(Outbreak::getCaseCount).reversed())
                .limit(5)
                .map(outbreak -> insightForOutbreak(outbreak, null))
                .toList();
    }

    private List<DoctorDashboardMetricInsightDto> nearestOutbreakInsights(List<Outbreak> outbreaks, HospitalGeoContextDto geoContext) {
        if (geoContext.getLatitude() == null || geoContext.getLongitude() == null) return List.of();
        double hospitalLatitude = geoContext.getLatitude().doubleValue();
        double hospitalLongitude = geoContext.getLongitude().doubleValue();
        return outbreaks.stream()
                .filter(outbreak -> outbreak.getMunicipality() != null)
                .filter(outbreak -> outbreak.getMunicipality().getLatitude() != null && outbreak.getMunicipality().getLongitude() != null)
                .sorted(Comparator.comparingDouble(outbreak -> distanceKm(
                        hospitalLatitude,
                        hospitalLongitude,
                        outbreak.getMunicipality().getLatitude().doubleValue(),
                        outbreak.getMunicipality().getLongitude().doubleValue())))
                .limit(5)
                .map(outbreak -> {
                    double distance = distanceKm(
                            hospitalLatitude,
                            hospitalLongitude,
                            outbreak.getMunicipality().getLatitude().doubleValue(),
                            outbreak.getMunicipality().getLongitude().doubleValue());
                    return insightForOutbreak(outbreak, "%.1f km from hospital".formatted(distance));
                })
                .toList();
    }

    private MunicipalityPriorityAggregate topPriorityMunicipality(List<Outbreak> outbreaks) {
        Map<String, MunicipalityPriorityAggregate> aggregates = new LinkedHashMap<>();
        for (Outbreak outbreak : outbreaks) {
            if (outbreak.getMunicipality() == null || outbreak.getDisease() == null) continue;
            DashboardSeverity severity = evaluateOutbreakSeverity(outbreak);
            if (severity == DashboardSeverity.LOW) continue;
            String key = outbreak.getMunicipality().getName() + "|" + outbreak.getMunicipality().getStateName();
            aggregates.computeIfAbsent(key, ignored -> new MunicipalityPriorityAggregate(
                    outbreak.getMunicipality().getName(),
                    outbreak.getMunicipality().getStateName())).add(outbreak, severity);
        }

        return aggregates.values().stream()
                .max(Comparator
                        .comparingInt(MunicipalityPriorityAggregate::outbreakCount)
                        .thenComparingInt(MunicipalityPriorityAggregate::caseCount)
                        .thenComparingInt(aggregate -> severityRank(aggregate.highestSeverity())))
                .orElse(null);
    }

    private List<DoctorDashboardMetricInsightDto> priorityMunicipalityInsights(
            List<Outbreak> outbreaks,
            MunicipalityPriorityAggregate priorityMunicipality
    ) {
        if (priorityMunicipality == null) return List.of();
        return outbreaks.stream()
                .filter(outbreak -> outbreak.getMunicipality() != null)
                .filter(outbreak -> outbreak.getDisease() != null)
                .filter(outbreak -> priorityMunicipality.municipalityName().equals(outbreak.getMunicipality().getName()))
                .filter(outbreak -> priorityMunicipality.stateName().equals(outbreak.getMunicipality().getStateName()))
                .filter(outbreak -> evaluateOutbreakSeverity(outbreak) != DashboardSeverity.LOW)
                .sorted(Comparator
                        .comparingInt((Outbreak outbreak) -> severityRank(evaluateOutbreakSeverity(outbreak))).reversed()
                        .thenComparing(Comparator.comparingInt(Outbreak::getCaseCount).reversed()))
                .limit(5)
                .map(outbreak -> insightForOutbreak(outbreak, "Priority municipality"))
                .toList();
    }

    private DoctorDashboardMetricInsightDto insightForOutbreak(Outbreak outbreak, String metaOverride) {
        DashboardSeverity severity = evaluateOutbreakSeverity(outbreak);
        String disease = outbreak.getDisease() == null ? "Unknown disease" : outbreak.getDisease().getName();
        return new DoctorDashboardMetricInsightDto(
                disease,
                locationWithState(outbreak),
                formatCount(outbreak.getCaseCount()) + (outbreak.getCaseCount() == 1 ? " case" : " cases"),
                severity.label(false),
                colorForSeverity(severity),
                metaOverride == null ? outbreak.getConfirmationStatus() : metaOverride);
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
                .sorted(Comparator
                        .comparingInt((Outbreak outbreak) -> severityRank(evaluateOutbreakSeverity(outbreak))).reversed()
                        .thenComparing(Comparator.comparingInt(Outbreak::getCaseCount).reversed()))
                .limit(24)
                .map(outbreak -> {
                    String disease = outbreak.getDisease().getName();
                    String location = locationLabel(outbreak);
                    DashboardSeverity severity = evaluateOutbreakSeverity(outbreak);
                    String variant = alertVariant(severity);
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
                            alertPriority(severity),
                            "Compare compatible patient symptoms against this active outbreak before closing the evaluation.",
                            outbreak.getCaseCount(),
                            formatCount(outbreak.getCaseCount()) + (outbreak.getCaseCount() == 1 ? " active case" : " active cases"),
                            outbreak.getConfirmationStatus(),
                            outbreak.getMunicipality() == null ? null : outbreak.getMunicipality().getName(),
                            outbreak.getMunicipality() == null ? (outbreak.getState() == null ? null : outbreak.getState().getName()) : outbreak.getMunicipality().getStateName());
                })
                .collect(Collectors.toList());
    }

    private List<DoctorDashboardZoneDto> buildZones(List<Outbreak> outbreaks, HospitalGeoContextDto geoContext, Hospital hospital) {
        List<DoctorDashboardZoneDto> zones = new ArrayList<>();
        if (geoContext != null && geoContext.getLatitude() != null && geoContext.getLongitude() != null) {
            String hospitalName = hospital == null || hospital.getName() == null || hospital.getName().isBlank()
                    ? "Hospital node"
                    : hospital.getName();
            zones.add(new DoctorDashboardZoneDto(
                    "hospital-node",
                    hospitalName,
                    "Monitored",
                    "Hospital context",
                    "Current facility",
                    "0 km",
                    "Operational review",
                    "Reference point for nearby outbreak context.",
                    "Use this point as the center of the doctor dashboard surveillance radius.",
                    geoContext.getMunicipalityName(),
                    geoContext.getStateName(),
                    geoContext.getLatitude(),
                    geoContext.getLongitude(),
                    "#0003B8"));
        }

        outbreaks.stream()
                .filter(outbreak -> outbreak.getDisease() != null)
                .filter(outbreak -> outbreak.getMunicipality() != null)
                .filter(outbreak -> outbreak.getMunicipality().getLatitude() != null && outbreak.getMunicipality().getLongitude() != null)
                .sorted(Comparator
                        .comparingInt((Outbreak outbreak) -> severityRank(evaluateOutbreakSeverity(outbreak))).reversed()
                        .thenComparing(Comparator.comparingInt(Outbreak::getCaseCount).reversed()))
                .forEach(outbreak -> {
                    DashboardSeverity severity = evaluateOutbreakSeverity(outbreak);
                    String radiusLabel = "";
                    if (geoContext != null
                            && geoContext.getLatitude() != null
                            && geoContext.getLongitude() != null
                            && outbreak.getMunicipality().getLatitude() != null
                            && outbreak.getMunicipality().getLongitude() != null) {
                        double distance = distanceKm(
                                geoContext.getLatitude().doubleValue(),
                                geoContext.getLongitude().doubleValue(),
                                outbreak.getMunicipality().getLatitude().doubleValue(),
                                outbreak.getMunicipality().getLongitude().doubleValue());
                        radiusLabel = "Within " + Math.max(1, Math.round(distance)) + " km";
                    }
                    zones.add(new DoctorDashboardZoneDto(
                            outbreak.getId().toString(),
                            outbreak.getMunicipality().getName(),
                            severity.label(false),
                            outbreak.getDisease().getName(),
                            formatCount(outbreak.getCaseCount()) + " active cases",
                            radiusLabel,
                            alertPriority(severity),
                            "Municipal outbreak signal in the hospital context radius.",
                            "Keep this disease in the differential when symptoms overlap.",
                            outbreak.getMunicipality().getName(),
                            outbreak.getMunicipality().getStateName(),
                            outbreak.getMunicipality().getLatitude(),
                            outbreak.getMunicipality().getLongitude(),
                            colorForSeverity(severity)));
                });
        return zones;
    }

    private DashboardSeverity evaluateContextSeverity(int totalCases, int outbreakCount) {
        if (outbreakCount >= 10 || totalCases >= 500) return DashboardSeverity.HIGH;
        if (outbreakCount >= 4 || totalCases >= 100) return DashboardSeverity.MODERATE;
        return DashboardSeverity.LOW;
    }

    private DashboardSeverity evaluateAggregateSeverity(String diseaseName, int caseCount, int outbreakCount) {
        if (outbreakCount >= 10 || caseCount >= 500 || isPriorityDisease(diseaseName) && caseCount >= 25) {
            return DashboardSeverity.HIGH;
        }
        if (outbreakCount >= 4 || caseCount >= 100 || caseCount >= 10) {
            return DashboardSeverity.MODERATE;
        }
        return DashboardSeverity.LOW;
    }

    private DashboardSeverity evaluateOutbreakSeverity(Outbreak outbreak) {
        int caseCount = outbreak.getCaseCount();
        String confirmationStatus = outbreak.getConfirmationStatus();
        String diseaseName = outbreak.getDisease() == null ? "" : outbreak.getDisease().getName();
        boolean confirmed = "CONFIRMED".equals(confirmationStatus);

        if (confirmed && caseCount >= 100) return DashboardSeverity.HIGH;
        if (confirmed && isPriorityDisease(diseaseName) && caseCount >= 25) return DashboardSeverity.HIGH;
        if (confirmed && caseCount >= 10) return DashboardSeverity.MODERATE;
        if (!confirmed && caseCount >= 25) return DashboardSeverity.MODERATE;
        return DashboardSeverity.LOW;
    }

    private String severityStatus(DashboardSeverity severity) {
        return switch (severity) {
            case HIGH -> "danger";
            case MODERATE -> "warning";
            case LOW -> "positive";
        };
    }

    private String alertVariant(DashboardSeverity severity) {
        return switch (severity) {
            case HIGH -> "critical";
            case MODERATE -> "warning";
            case LOW -> "success";
        };
    }

    private String alertPriority(DashboardSeverity severity) {
        return switch (severity) {
            case HIGH -> "Immediate";
            case MODERATE -> "High";
            case LOW -> "Routine";
        };
    }

    private int severityRank(DashboardSeverity severity) {
        return switch (severity) {
            case HIGH -> 3;
            case MODERATE -> 2;
            case LOW -> 1;
        };
    }

    private String colorForSeverity(DashboardSeverity severity) {
        return switch (severity) {
            case HIGH -> "#EF4444";
            case MODERATE -> "#F97316";
            case LOW -> "#22C55E";
        };
    }

    private boolean isPriorityDisease(String diseaseName) {
        return PRIORITY_DISEASES.contains(normalizeDiseaseName(diseaseName));
    }

    private String normalizeDiseaseName(String diseaseName) {
        String normalized = java.text.Normalizer.normalize(diseaseName == null ? "" : diseaseName, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]+", " ")
                .trim()
                .toLowerCase();
        if ("hiv aids".equals(normalized) || "hiv".equals(normalized)) return "hiv aids";
        if ("covid 19".equals(normalized) || "covid".equals(normalized)) return "covid 19";
        return normalized;
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

    private String locationWithState(Outbreak outbreak) {
        if (outbreak.getMunicipality() != null) {
            String stateName = outbreak.getMunicipality().getStateName();
            return stateName == null || stateName.isBlank()
                    ? outbreak.getMunicipality().getName()
                    : outbreak.getMunicipality().getName() + ", " + stateName;
        }
        if (outbreak.getState() != null) {
            return outbreak.getState().getName();
        }
        return "hospital region";
    }

    private String stateName(Outbreak outbreak) {
        if (outbreak.getState() != null && outbreak.getState().getName() != null) {
            return outbreak.getState().getName();
        }
        if (outbreak.getMunicipality() != null) {
            return outbreak.getMunicipality().getStateName();
        }
        return null;
    }

    private double distanceKm(double latitudeA, double longitudeA, double latitudeB, double longitudeB) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(latitudeB - latitudeA);
        double dLon = Math.toRadians(longitudeB - longitudeA);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latitudeA)) * Math.cos(Math.toRadians(latitudeB))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
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

    private static class MunicipalityPriorityAggregate {
        private final String municipalityName;
        private final String stateName;
        private int outbreakCount;
        private int caseCount;
        private DashboardSeverity highestSeverity = DashboardSeverity.LOW;

        MunicipalityPriorityAggregate(String municipalityName, String stateName) {
            this.municipalityName = municipalityName;
            this.stateName = stateName;
        }

        void add(Outbreak outbreak, DashboardSeverity severity) {
            outbreakCount++;
            caseCount += outbreak.getCaseCount();
            if (severityRankValue(severity) > severityRankValue(highestSeverity)) {
                highestSeverity = severity;
            }
        }

        String municipalityName() { return municipalityName; }
        String stateName() { return stateName; }
        int outbreakCount() { return outbreakCount; }
        int caseCount() { return caseCount; }
        DashboardSeverity highestSeverity() { return highestSeverity; }

        private static int severityRankValue(DashboardSeverity severity) {
            return switch (severity) {
                case HIGH -> 3;
                case MODERATE -> 2;
                case LOW -> 1;
            };
        }
    }

    public record DoctorDashboardStateMapDto(
            UUID stateId,
            String stateName,
            double latitude,
            double longitude,
            int outbreakCount,
            int caseCount
    ) {}

    public record DoctorDashboardReportDto(
            String scope,
            String hospitalName,
            String municipalityName,
            String stateName,
            LocalDateTime generatedAt,
            List<DoctorDashboardReportOutbreakDto> outbreaks
    ) {}

    public record DoctorDashboardReportOutbreakDto(
            UUID id,
            String diseaseName,
            String location,
            String scope,
            int caseCount,
            String confirmationStatus,
            LocalDateTime startedAt
    ) {}

    private static class StateAggregate {
        private final UUID stateId;
        private final String stateName;
        private double latitudeSum;
        private double longitudeSum;
        private int municipalityCount;
        private int outbreakCount;
        private int caseCount;

        StateAggregate(UUID stateId, String stateName) {
            this.stateId = stateId;
            this.stateName = stateName;
        }

        void addMunicipality(Municipality municipality) {
            latitudeSum += municipality.getLatitude().doubleValue();
            longitudeSum += municipality.getLongitude().doubleValue();
            municipalityCount++;
        }

        void addOutbreak(Outbreak outbreak) {
            outbreakCount++;
            caseCount += outbreak.getCaseCount();
        }

        boolean hasCoordinates() {
            return municipalityCount > 0;
        }

        String stateName() {
            return stateName;
        }

        DoctorDashboardStateMapDto toDto() {
            return new DoctorDashboardStateMapDto(
                    stateId,
                    stateName,
                    latitudeSum / municipalityCount,
                    longitudeSum / municipalityCount,
                    outbreakCount,
                    caseCount);
        }
    }

    private enum DashboardSeverity {
        LOW,
        MODERATE,
        HIGH;

        String label(boolean clearWhenLow) {
            return switch (this) {
                case HIGH -> "High";
                case MODERATE -> "Moderate";
                case LOW -> clearWhenLow ? "Clear" : "Low";
            };
        }
    }
}
