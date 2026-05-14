package com.itesm.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itesm.application.dto.HospitalGeoContextDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.Hospital;
import com.itesm.domain.models.HospitalDepartmentResource;
import com.itesm.domain.models.HospitalInventoryItem;
import com.itesm.domain.models.HospitalResourceSnapshot;
import com.itesm.domain.models.HospitalStaffingProfile;
import com.itesm.domain.models.OperationalRecommendation;
import com.itesm.domain.models.OperationalRecommendationAudit;
import com.itesm.domain.models.Outbreak;
import com.itesm.domain.repository.HospitalRepository;
import com.itesm.domain.repository.HospitalResourceRepository;
import com.itesm.domain.repository.OperationalRecommendationRepository;
import com.itesm.domain.repository.OutbreakRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Deterministic recommendation engine for hospital admin operations.
 *
 * Facts and suggested quantities come from operational signals and fixed rules.
 * LLM summarization can be layered on top later without changing the stored
 * recommendation workflow.
 */
@ApplicationScoped
public class RefreshOperationalRecommendationsUseCase {

    private static final UUID NO_MUNICIPALITY_MATCH = new UUID(0, 0);

    @Inject AuthenticatedUserContext authenticatedUserContext;
    @Inject HospitalRepository hospitalRepository;
    @Inject OutbreakRepository outbreakRepository;
    @Inject HospitalGeoContextService hospitalGeoContextService;
    @Inject HospitalResourceRepository resourceRepository;
    @Inject OperationalRecommendationRepository recommendationRepository;
    @Inject EntityManager entityManager;
    @Inject ObjectMapper objectMapper;
    @Inject RecommendationWorkflowPolicyService workflowPolicyService;
    @Inject OperationalRecommendationNarrativeComposer recommendationNarrativeComposer;

    @Transactional
    public int execute() {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        UUID hospitalId = currentUser.getHospitalId();
        if (hospitalId == null) {
            throw new NotFoundException("User has no assigned hospital");
        }
        return executeForHospital(hospitalId);
    }

    @Transactional
    public int executeForAllHospitals() {
        int generated = 0;
        for (Hospital hospital : hospitalRepository.listAllDomain()) {
            if (!hospital.isActive()) {
                continue;
            }
            generated += executeForHospital(hospital.getId());
        }
        return generated;
    }

    @Transactional
    public int executeForHospital(UUID hospitalId) {
        Hospital hospital = hospitalRepository.findHospitalById(hospitalId)
                .orElseThrow(() -> new NotFoundException("Hospital not found: " + hospitalId));

        HospitalGeoContextDto geoContext = hospitalGeoContextService.resolve(hospital);
        List<Outbreak> outbreaks = outbreakRepository.findActiveByMunicipalityIdsOrStateId(
                geoContext.getIncludedMunicipalityIds(),
                geoContext.getStateId());

        HospitalResourceSnapshot snapshot = resourceRepository
                .findLatestSnapshotByHospitalId(hospitalId)
                .orElse(null);
        List<HospitalDepartmentResource> departments = resourceRepository.findDepartmentsByHospitalId(hospitalId);
        List<HospitalStaffingProfile> staffingProfiles = resourceRepository.findStaffingByHospitalId(hospitalId);
        List<HospitalInventoryItem> inventoryItems = resourceRepository.findInventoryByHospitalId(hospitalId);

        int activeAlertCount = countOpenAlerts(geoContext);
        int activeEventCount = countActiveEvents(hospitalId);
        int recentEvaluationCount = countRecentEvaluations(hospitalId, LocalDateTime.now().minusDays(7));
        int criticalInventoryCount = countCriticalInventoryItems(hospitalId);
        UUID primaryAlertId = findTopOpenAlertId(geoContext).orElse(null);

        RecommendationSignals signals = new RecommendationSignals(
                activeAlertCount,
                activeEventCount,
                recentEvaluationCount,
                criticalInventoryCount
        );

        List<OperationalRecommendation> generated = buildRecommendations(
                hospitalId,
                geoContext,
                snapshot,
                departments,
                staffingProfiles,
                inventoryItems,
                outbreaks,
                signals,
                primaryAlertId
        );

        return persistRecommendations(generated);
    }

    private List<OperationalRecommendation> buildRecommendations(
            UUID hospitalId,
            HospitalGeoContextDto geoContext,
            HospitalResourceSnapshot snapshot,
            List<HospitalDepartmentResource> departments,
            List<HospitalStaffingProfile> staffingProfiles,
            List<HospitalInventoryItem> inventoryItems,
            List<Outbreak> outbreaks,
            RecommendationSignals signals,
            UUID primaryAlertId) {

        List<OperationalRecommendation> generated = new ArrayList<>();
        Outbreak worstOutbreak = outbreaks.stream()
                .max(Comparator.comparingInt(Outbreak::getCaseCount))
                .orElse(null);

        long highSeverityOutbreaks = outbreaks.stream()
                .filter(o -> "ACTIVE".equalsIgnoreCase(o.getStatus()) && o.getCaseCount() > 50)
                .count();

        boolean hasRespiratoryOutbreak = outbreaks.stream()
                .filter(o -> "ACTIVE".equalsIgnoreCase(o.getStatus()))
                .anyMatch(o -> o.getDisease() != null && isRespiratoryDisease(o.getDisease().getName()));

        if (snapshot != null && snapshot.getTotalBeds() > 0) {
            double occupancyRate = 1.0 - ((double) snapshot.getAvailableBeds() / snapshot.getTotalBeds());
            if (occupancyRate >= 0.85) {
                int additionalBeds = Math.max(5, snapshot.getTotalBeds() / 10);
                generated.add(buildRecommendation(
                        hospitalId,
                        primaryAlertId,
                        worstOutbreak != null ? worstOutbreak.getId() : null,
                        "BED_CAPACITY",
                        occupancyRate >= 0.92 || signals.activeEventCount() >= 20 ? "CRITICAL" : "HIGH",
                        "Expand Monitored Bed Capacity",
                        "Hospital bed occupancy is at " + Math.round(occupancyRate * 100) + "%. " +
                                "Open additional monitored beds to prevent capacity overflow.",
                        "Reduce patient wait times and prevent diversion",
                        signals.recentEvaluationCount() >= 12 ? "Within 12 hours" : "Within 24 hours",
                        occupancyRate >= 0.92 ? new BigDecimal("0.95") : new BigDecimal("0.90"),
                        List.of(
                                "Open " + additionalBeds + " additional monitored beds",
                                "Prepare overflow routing for low-acuity admissions",
                                "Reconfirm discharge readiness for stable patients"
                        ),
                        List.of("General Ward", "ICU"),
                        List.of("Beds", "Nursing Staff"),
                        buildInputContextJson(geoContext, snapshot, outbreaks, signals)
                ));
            } else if (occupancyRate >= 0.70 && signals.recentEvaluationCount() >= 8) {
                generated.add(buildRecommendation(
                        hospitalId,
                        primaryAlertId,
                        worstOutbreak != null ? worstOutbreak.getId() : null,
                        "BED_CAPACITY",
                        "HIGH",
                        "Monitor Bed Occupancy Trend",
                        "Hospital bed occupancy has reached " + Math.round(occupancyRate * 100) + "%. " +
                                "Begin contingency planning to avoid reaching critical capacity.",
                        "Prevent critical bed shortage",
                        "Within 48 hours",
                        new BigDecimal("0.82"),
                        List.of(
                                "Prepare overflow protocol for additional " + Math.max(3, snapshot.getTotalBeds() / 15) + " beds",
                                "Review discharge coordination before next surge window"
                        ),
                        List.of("General Ward"),
                        List.of("Beds"),
                        buildInputContextJson(geoContext, snapshot, outbreaks, signals)
                ));
            }
        }

        if (highSeverityOutbreaks >= 2 && snapshot != null && snapshot.getDoctorsOnShift() > 0) {
            int expectedDoctors = (int) Math.ceil(Math.max(snapshot.getTotalBeds(), 1) / 8.0);
            if (snapshot.getDoctorsOnShift() < expectedDoctors || signals.activeEventCount() >= 18) {
                int gap = Math.max(expectedDoctors - snapshot.getDoctorsOnShift(), 1);
                generated.add(buildRecommendation(
                        hospitalId,
                        primaryAlertId,
                        worstOutbreak != null ? worstOutbreak.getId() : null,
                        "STAFFING",
                        signals.activeAlertCount() >= 2 ? "CRITICAL" : "HIGH",
                        "Increase Emergency Physician Staffing",
                        "Multiple high-severity outbreaks detected nearby and current physician coverage is below the recommended ratio for outbreak pressure.",
                        "Improve patient throughput during outbreak surge",
                        "Next staffing rotation",
                        new BigDecimal("0.87"),
                        List.of(
                                "Activate " + gap + " additional on-call physicians",
                                "Redirect coverage from low-urgency departments",
                                "Prepare respiratory-capable backup staffing"
                        ),
                        List.of("Emergency Department", "ICU"),
                        List.of("Physician Staff", "On-Call Roster"),
                        buildInputContextJson(geoContext, snapshot, outbreaks, signals)
                ));
            }
        }

        if (snapshot != null && snapshot.getIcuTotalBeds() > 0) {
            double icuOccupancy = 1.0 - ((double) snapshot.getIcuAvailableBeds() / snapshot.getIcuTotalBeds());
            if (icuOccupancy >= 0.80) {
                generated.add(buildRecommendation(
                        hospitalId,
                        primaryAlertId,
                        worstOutbreak != null ? worstOutbreak.getId() : null,
                        "BED_CAPACITY",
                        "CRITICAL",
                        "ICU Capacity Critical - Activate Surge Protocol",
                        "ICU occupancy is at " + Math.round(icuOccupancy * 100) + "% (" +
                                snapshot.getIcuAvailableBeds() + " beds remaining). Immediate action required.",
                        "Prevent ICU overflow and ensure critical care availability",
                        "Immediately",
                        new BigDecimal("0.95"),
                        List.of(
                                "Activate ICU surge protocol",
                                "Convert step-down units to monitored ICU capacity",
                                "Expedite eligible ICU discharges"
                        ),
                        List.of("ICU", "Step-Down Unit"),
                        List.of("ICU Beds", "Ventilators", "ICU Nursing"),
                        buildInputContextJson(geoContext, snapshot, outbreaks, signals)
                ));
            }
        }

        if (hasRespiratoryOutbreak) {
            generated.add(buildRecommendation(
                    hospitalId,
                    primaryAlertId,
                    worstOutbreak != null ? worstOutbreak.getId() : null,
                    "ISOLATION",
                    signals.activeAlertCount() > 0 ? "CRITICAL" : "HIGH",
                    "Activate Respiratory Isolation Protocol",
                    "Active respiratory disease outbreak detected in the hospital catchment area. Establish dedicated respiratory isolation zones to prevent nosocomial spread.",
                    "Prevent in-hospital transmission to staff and other patients",
                    "Within 12 hours",
                    new BigDecimal("0.88"),
                    List.of(
                            "Designate a respiratory isolation wing",
                            "Enforce N95 protocols",
                            "Screen incoming patients for respiratory symptoms at triage"
                    ),
                    List.of("Emergency Department", "Respiratory Ward", "General Admission"),
                    List.of("Isolation Rooms", "PPE Stock", "Negative Pressure Equipment"),
                    buildInputContextJson(geoContext, snapshot, outbreaks, signals)
            ));
        }

        if (signals.criticalInventoryCount() > 0 || (hasRespiratoryOutbreak && snapshot != null && snapshot.getOxygenAvailableUnits() < snapshot.getOxygenCapacityUnits() * 0.35)) {
            generated.add(buildRecommendation(
                    hospitalId,
                    primaryAlertId,
                    worstOutbreak != null ? worstOutbreak.getId() : null,
                    "SUPPLY",
                    signals.criticalInventoryCount() >= 2 ? "CRITICAL" : "HIGH",
                    "Replenish Critical Protective and Respiratory Supplies",
                    "Critical inventory levels and respiratory outbreak pressure indicate a near-term supply risk for isolation response capacity.",
                    "Maintain staff protection and respiratory surge readiness",
                    "Within 24 hours",
                    new BigDecimal("0.84"),
                    List.of(
                            "Place an expedited replenishment order for critical PPE",
                            "Audit oxygen reserves and respiratory consumables",
                            "Reserve remaining stock for outbreak response areas"
                    ),
                    List.of("Central Supply", "Respiratory Ward", "Emergency Department"),
                    List.of("N95 Masks", "Isolation Gowns", "Oxygen Supplies"),
                    buildInputContextJson(geoContext, snapshot, outbreaks, signals)
            ));
        }

        generated.forEach(recommendation -> {
            recommendationNarrativeComposer.enhance(recommendation);
            workflowPolicyService.populateDefaults(recommendation, departments, staffingProfiles, inventoryItems);
        });

        return generated;
    }

    private int persistRecommendations(List<OperationalRecommendation> recommendations) {
        int generatedCount = 0;
        for (OperationalRecommendation recommendation : recommendations) {
            Optional<OperationalRecommendation> existing = recommendationRepository.findOpenByHospitalIdAndTypeAndTitle(
                    recommendation.getHospitalId(),
                    recommendation.getType(),
                    recommendation.getTitle()
            );

            boolean isNew = existing.isEmpty();
            if (existing.isPresent()) {
                OperationalRecommendation current = existing.get();
                recommendation.setId(current.getId());
                recommendation.setCreatedAt(current.getCreatedAt());
                recommendation.setStatus(current.getStatus());
                recommendation.setResolvedAt(null);
                if (recommendation.getSourceAlertId() == null) {
                    recommendation.setSourceAlertId(current.getSourceAlertId());
                }
                if (recommendation.getSourceOutbreakId() == null) {
                    recommendation.setSourceOutbreakId(current.getSourceOutbreakId());
                }
            }

            OperationalRecommendation saved = recommendationRepository.save(recommendation);

            OperationalRecommendationAudit audit = new OperationalRecommendationAudit();
            audit.setRecommendationId(saved.getId());
            audit.setEventType("GENERATED");
            audit.setEventLabel(isNew
                    ? "Recommendation generated by rule engine"
                    : "Recommendation refreshed with latest operational context");
            audit.setCreatedAt(LocalDateTime.now());
            recommendationRepository.appendAudit(audit);

            if (isNew) {
                generatedCount++;
            }
        }

        return generatedCount;
    }

    private int countOpenAlerts(HospitalGeoContextDto geoContext) {
        return entityManager.createQuery("""
                select count(a)
                from AlertEntity a
                join a.outbreak o
                left join o.municipality m
                left join o.state s
                where a.acknowledgedAt is null
                  and o.status = 'ACTIVE'
                  and (
                      (:hasMunicipalities = true and o.scope = 'MUNICIPALITY' and m.id in :municipalityIds)
                      or (:hasState = true and o.scope = 'STATE' and s.id = :stateId)
                  )
                """, Long.class)
                .setParameter("municipalityIds", queryMunicipalityIds(geoContext.getIncludedMunicipalityIds()))
                .setParameter("stateId", geoContext.getStateId())
                .setParameter("hasMunicipalities", geoContext.getIncludedMunicipalityIds() != null && !geoContext.getIncludedMunicipalityIds().isEmpty())
                .setParameter("hasState", geoContext.getStateId() != null)
                .getSingleResult()
                .intValue();
    }

    private Optional<UUID> findTopOpenAlertId(HospitalGeoContextDto geoContext) {
        return entityManager.createQuery("""
                select a.id
                from AlertEntity a
                join a.outbreak o
                left join o.municipality m
                left join o.state s
                where a.acknowledgedAt is null
                  and o.status = 'ACTIVE'
                  and (
                      (:hasMunicipalities = true and o.scope = 'MUNICIPALITY' and m.id in :municipalityIds)
                      or (:hasState = true and o.scope = 'STATE' and s.id = :stateId)
                  )
                order by
                  case a.severity when 'HIGH' then 3 when 'MEDIUM' then 2 else 1 end desc,
                  a.createdAt desc
                """, UUID.class)
                .setParameter("municipalityIds", queryMunicipalityIds(geoContext.getIncludedMunicipalityIds()))
                .setParameter("stateId", geoContext.getStateId())
                .setParameter("hasMunicipalities", geoContext.getIncludedMunicipalityIds() != null && !geoContext.getIncludedMunicipalityIds().isEmpty())
                .setParameter("hasState", geoContext.getStateId() != null)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }

    private int countActiveEvents(UUID hospitalId) {
        return entityManager.createQuery("""
                select count(e)
                from EventEntity e
                where e.status = 'ACTIVE'
                  and e.primaryDoctor.hospital.id = :hospitalId
                """, Long.class)
                .setParameter("hospitalId", hospitalId)
                .getSingleResult()
                .intValue();
    }

    private int countRecentEvaluations(UUID hospitalId, LocalDateTime since) {
        return entityManager.createQuery("""
                select count(pe)
                from PatientEvaluationEntity pe
                where pe.doctor.hospital.id = :hospitalId
                  and pe.createdAt >= :since
                """, Long.class)
                .setParameter("hospitalId", hospitalId)
                .setParameter("since", since)
                .getSingleResult()
                .intValue();
    }

    private int countCriticalInventoryItems(UUID hospitalId) {
        return (int) resourceRepository.findInventoryByHospitalId(hospitalId).stream()
                .filter(item -> item.getStatus() != null)
                .filter(item -> "CRITICAL".equalsIgnoreCase(item.getStatus()) || "LOW".equalsIgnoreCase(item.getStatus()))
                .count();
    }

    private List<UUID> queryMunicipalityIds(List<UUID> municipalityIds) {
        if (municipalityIds == null || municipalityIds.isEmpty()) {
            return List.of(NO_MUNICIPALITY_MATCH);
        }
        return municipalityIds;
    }

    private boolean isRespiratoryDisease(String diseaseName) {
        String normalized = diseaseName == null ? "" : diseaseName.toLowerCase();
        return normalized.contains("covid")
                || normalized.contains("influenza")
                || normalized.contains("tuberculosis")
                || normalized.contains("respiratory")
                || normalized.contains("pneumonia");
    }

    private OperationalRecommendation buildRecommendation(
            UUID hospitalId,
            UUID alertId,
            UUID outbreakId,
            String type,
            String severity,
            String title,
            String description,
            String expectedImpact,
            String urgencyWindow,
            BigDecimal confidence,
            List<String> recommendedActions,
            List<String> affectedDepartments,
            List<String> affectedResources,
            String inputContextJson) {

        OperationalRecommendation recommendation = new OperationalRecommendation();
        recommendation.setHospitalId(hospitalId);
        recommendation.setSourceAlertId(alertId);
        recommendation.setSourceOutbreakId(outbreakId);
        recommendation.setType(type);
        recommendation.setSeverity(severity);
        recommendation.setStatus("NEW");
        recommendation.setCategory(type);
        recommendation.setTitle(title);
        recommendation.setDescription(description);
        recommendation.setExpectedImpact(expectedImpact);
        recommendation.setUrgencyWindow(urgencyWindow);
        recommendation.setConfidenceScore(confidence);
        recommendation.setRationaleJson(serializeJson(List.of(description)));
        recommendation.setRecommendedActionsJson(serializeJson(recommendedActions));
        recommendation.setAffectedDepartmentsJson(serializeJson(affectedDepartments));
        recommendation.setAffectedResourcesJson(serializeJson(affectedResources));
        recommendation.setModelProvider("deterministic-rule-engine");
        recommendation.setModelVersion("v1");
        recommendation.setInputContextJson(inputContextJson);
        recommendation.setCreatedByMode("RULE_ENGINE");
        recommendation.setCreatedAt(LocalDateTime.now());
        recommendation.setUpdatedAt(LocalDateTime.now());
        return recommendation;
    }

    private String buildInputContextJson(
            HospitalGeoContextDto geoContext,
            HospitalResourceSnapshot snapshot,
            List<Outbreak> outbreaks,
            RecommendationSignals signals) {
        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> geoScope = new LinkedHashMap<>();
        geoScope.put("hospitalId", geoContext.getHospitalId() != null ? geoContext.getHospitalId().toString() : null);
        geoScope.put("municipalityId", geoContext.getMunicipalityId() != null ? geoContext.getMunicipalityId().toString() : null);
        geoScope.put("stateId", geoContext.getStateId() != null ? geoContext.getStateId().toString() : null);
        geoScope.put("radiusKm", geoContext.getRadiusKm());

        payload.put("generatedAt", LocalDateTime.now().toString());
        payload.put("hospitalGeoScope", geoScope);
        payload.put("signals", Map.of(
                "activeAlertCount", signals.activeAlertCount(),
                "activeEventCount", signals.activeEventCount(),
                "recentEvaluationCount", signals.recentEvaluationCount(),
                "criticalInventoryCount", signals.criticalInventoryCount(),
                "activeOutbreakCount", outbreaks.size()
        ));
        payload.put("outbreaks", outbreaks.stream().limit(10).map(outbreak -> {
            Map<String, Object> outbreakPayload = new LinkedHashMap<>();
            outbreakPayload.put("id", outbreak.getId() != null ? outbreak.getId().toString() : null);
            outbreakPayload.put("diseaseName", outbreak.getDisease() != null ? outbreak.getDisease().getName() : "Unknown");
            outbreakPayload.put("scope", outbreak.getScope());
            outbreakPayload.put("status", outbreak.getStatus());
            outbreakPayload.put("caseCount", outbreak.getCaseCount());
            return outbreakPayload;
        }).toList());
        if (snapshot != null) {
            Map<String, Object> snapshotPayload = new LinkedHashMap<>();
            snapshotPayload.put("totalBeds", snapshot.getTotalBeds());
            snapshotPayload.put("availableBeds", snapshot.getAvailableBeds());
            snapshotPayload.put("icuTotalBeds", snapshot.getIcuTotalBeds());
            snapshotPayload.put("icuAvailableBeds", snapshot.getIcuAvailableBeds());
            snapshotPayload.put("oxygenCapacityUnits", snapshot.getOxygenCapacityUnits());
            snapshotPayload.put("oxygenAvailableUnits", snapshot.getOxygenAvailableUnits());
            snapshotPayload.put("doctorsOnShift", snapshot.getDoctorsOnShift());
            snapshotPayload.put("nursesOnShift", snapshot.getNursesOnShift());
            snapshotPayload.put("specialistsOnShift", snapshot.getSpecialistsOnShift());
            snapshotPayload.put("capturedAt", snapshot.getCapturedAt() != null ? snapshot.getCapturedAt().toString() : null);
            payload.put("snapshot", snapshotPayload);
        }
        return serializeJson(payload);
    }

    private String serializeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize recommendation context", e);
        }
    }

    private record RecommendationSignals(
            int activeAlertCount,
            int activeEventCount,
            int recentEvaluationCount,
            int criticalInventoryCount) {
    }
}
