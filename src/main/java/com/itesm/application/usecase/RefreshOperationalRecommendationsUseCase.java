package com.itesm.application.usecase;

import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.Hospital;
import com.itesm.domain.models.HospitalResourceSnapshot;
import com.itesm.domain.models.OperationalRecommendation;
import com.itesm.domain.models.OperationalRecommendationAudit;
import com.itesm.domain.models.Outbreak;
import com.itesm.domain.repository.HospitalRepository;
import com.itesm.domain.repository.HospitalResourceRepository;
import com.itesm.domain.repository.OperationalRecommendationRepository;
import com.itesm.domain.repository.OutbreakRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Phase 1 rule engine: generates recommendations based on outbreak signals + resource state.
 * LLM summarization can be layered on top later (Phase 3).
 */
@ApplicationScoped
public class RefreshOperationalRecommendationsUseCase {

    @Inject AuthenticatedUserContext authenticatedUserContext;
    @Inject HospitalRepository hospitalRepository;
    @Inject OutbreakRepository outbreakRepository;
    @Inject HospitalGeoContextService hospitalGeoContextService;
    @Inject HospitalResourceRepository resourceRepository;
    @Inject OperationalRecommendationRepository recommendationRepository;

    @Transactional
    public int execute() {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        UUID hospitalId = currentUser.getHospitalId();
        if (hospitalId == null) throw new NotFoundException("User has no assigned hospital");

        Hospital hospital = hospitalRepository.findHospitalById(hospitalId)
                .orElseThrow(() -> new NotFoundException("Hospital not found: " + hospitalId));

        var geoContext = hospitalGeoContextService.resolve(hospital);
        List<Outbreak> outbreaks = outbreakRepository.findActiveByMunicipalityIds(geoContext.getIncludedMunicipalityIds());

        HospitalResourceSnapshot snapshot = resourceRepository
                .findLatestSnapshotByHospitalId(hospitalId).orElse(null);

        List<OperationalRecommendation> generated = new ArrayList<>();

        // --- Rule 1: Bed capacity under pressure ---
        if (snapshot != null && snapshot.getTotalBeds() > 0) {
            double occupancyRate = 1.0 - ((double) snapshot.getAvailableBeds() / snapshot.getTotalBeds());
            if (occupancyRate >= 0.85) {
                generated.add(buildRecommendation(hospitalId, null, null,
                        "BED_CAPACITY", "CRITICAL",
                        "Expand Monitored Bed Capacity",
                        "Hospital bed occupancy is at " + Math.round(occupancyRate * 100) + "%. " +
                                "Open additional monitored beds to prevent capacity overflow.",
                        "Reduce patient wait times and prevent diversion",
                        "Within 24 hours",
                        new BigDecimal("0.92"),
                        "Open " + Math.max(5, snapshot.getTotalBeds() / 10) + " additional monitored beds",
                        "General Ward; ICU",
                        "Beds; Nursing Staff"));
            } else if (occupancyRate >= 0.70) {
                generated.add(buildRecommendation(hospitalId, null, null,
                        "BED_CAPACITY", "HIGH",
                        "Monitor Bed Occupancy Trend",
                        "Hospital bed occupancy has reached " + Math.round(occupancyRate * 100) + "%. " +
                                "Begin contingency planning to avoid reaching critical capacity.",
                        "Prevent critical bed shortage",
                        "Within 48 hours",
                        new BigDecimal("0.80"),
                        "Prepare overflow protocol for additional " + Math.max(3, snapshot.getTotalBeds() / 15) + " beds",
                        "General Ward",
                        "Beds"));
            }
        }

        // --- Rule 2: High-severity outbreak nearby with staffing gap ---
        long highSeverityOutbreaks = outbreaks.stream()
                .filter(o -> "ACTIVE".equalsIgnoreCase(o.getStatus()) && o.getCaseCount() > 50)
                .count();
        if (highSeverityOutbreaks >= 2 && snapshot != null && snapshot.getDoctorsOnShift() > 0) {
            int expectedDoctors = (int) Math.ceil(snapshot.getTotalBeds() / 8.0);
            if (snapshot.getDoctorsOnShift() < expectedDoctors) {
                int gap = expectedDoctors - snapshot.getDoctorsOnShift();
                Outbreak worstOutbreak = outbreaks.stream()
                        .filter(o -> "ACTIVE".equalsIgnoreCase(o.getStatus()))
                        .max((a, b) -> Integer.compare(a.getCaseCount(), b.getCaseCount()))
                        .orElse(null);

                generated.add(buildRecommendation(hospitalId,
                        null,
                        worstOutbreak != null ? worstOutbreak.getId() : null,
                        "STAFFING", "HIGH",
                        "Increase Emergency Physician Staffing",
                        "Multiple high-severity outbreaks detected nearby with " + highSeverityOutbreaks +
                                " active outbreak clusters exceeding 50 cases each. Current doctor-to-bed ratio is insufficient.",
                        "Improve patient throughput during outbreak surge",
                        "Next staffing rotation",
                        new BigDecimal("0.85"),
                        "Activate " + gap + " additional on-call physicians; redirect from low-urgency departments",
                        "Emergency Department; ICU",
                        "Physician Staff; On-Call Roster"));
            }
        }

        // --- Rule 3: ICU near capacity ---
        if (snapshot != null && snapshot.getIcuTotalBeds() > 0) {
            double icuOccupancy = 1.0 - ((double) snapshot.getIcuAvailableBeds() / snapshot.getIcuTotalBeds());
            if (icuOccupancy >= 0.80) {
                generated.add(buildRecommendation(hospitalId, null, null,
                        "BED_CAPACITY", "CRITICAL",
                        "ICU Capacity Critical - Activate Surge Protocol",
                        "ICU occupancy is at " + Math.round(icuOccupancy * 100) + "% (" +
                                snapshot.getIcuAvailableBeds() + " beds remaining). Immediate action required.",
                        "Prevent ICU overflow and ensure critical care availability",
                        "Immediately",
                        new BigDecimal("0.95"),
                        "Activate ICU surge protocol; convert step-down units; expedite discharges",
                        "ICU; Step-Down Unit",
                        "ICU Beds; Ventilators; ICU Nursing"));
            }
        }

        // --- Rule 4: Supply / isolation for outbreak disease ---
        boolean hasRespiratoryOutbreak = outbreaks.stream()
                .filter(o -> "ACTIVE".equalsIgnoreCase(o.getStatus()))
                .anyMatch(o -> o.getDisease() != null &&
                        (o.getDisease().getName().toLowerCase().contains("covid") ||
                         o.getDisease().getName().toLowerCase().contains("influenza") ||
                         o.getDisease().getName().toLowerCase().contains("tuberculosis")));
        if (hasRespiratoryOutbreak) {
            generated.add(buildRecommendation(hospitalId, null, null,
                    "ISOLATION", "HIGH",
                    "Activate Respiratory Isolation Protocol",
                    "Active respiratory disease outbreak detected in the hospital catchment area. " +
                            "Establish dedicated respiratory isolation zones to prevent nosocomial spread.",
                    "Prevent in-hospital transmission to staff and other patients",
                    "Within 12 hours",
                    new BigDecimal("0.88"),
                    "Designate isolation wing; enforce N95 protocols; screen incoming patients",
                    "Emergency Department; Respiratory Ward; General Admission",
                    "Isolation Rooms; PPE Stock; Negative Pressure Equipment"));
        }

        // Persist and audit each new recommendation
        int count = 0;
        for (OperationalRecommendation rec : generated) {
            OperationalRecommendation saved = recommendationRepository.save(rec);
            OperationalRecommendationAudit audit = new OperationalRecommendationAudit();
            audit.setRecommendationId(saved.getId());
            audit.setEventType("GENERATED");
            audit.setEventLabel("Recommendation generated by rule engine");
            audit.setCreatedAt(LocalDateTime.now());
            recommendationRepository.appendAudit(audit);
            count++;
        }

        return count;
    }

    private OperationalRecommendation buildRecommendation(
            UUID hospitalId, UUID alertId, UUID outbreakId,
            String type, String severity,
            String title, String description,
            String expectedImpact, String urgencyWindow,
            BigDecimal confidence,
            String recommendedActionsText,
            String affectedDepartments,
            String affectedResources) {

        OperationalRecommendation rec = new OperationalRecommendation();
        rec.setHospitalId(hospitalId);
        rec.setSourceAlertId(alertId);
        rec.setSourceOutbreakId(outbreakId);
        rec.setType(type);
        rec.setSeverity(severity);
        rec.setStatus("NEW");
        rec.setCategory(type);
        rec.setTitle(title);
        rec.setDescription(description);
        rec.setExpectedImpact(expectedImpact);
        rec.setUrgencyWindow(urgencyWindow);
        rec.setConfidenceScore(confidence);
        rec.setRationaleJson("[\"" + description.replace("\"", "'") + "\"]");
        rec.setRecommendedActionsJson("[\"" + recommendedActionsText.replace("\"", "'") + "\"]");
        rec.setAffectedDepartmentsJson("[\"" + affectedDepartments.replace("\"", "'") + "\"]");
        rec.setAffectedResourcesJson("[\"" + affectedResources.replace("\"", "'") + "\"]");
        rec.setCreatedByMode("RULE_ENGINE");
        rec.setCreatedAt(LocalDateTime.now());
        rec.setUpdatedAt(LocalDateTime.now());
        return rec;
    }
}
