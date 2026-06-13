package com.itesm.application.usecase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itesm.application.dto.AdminDashboardSummaryDto;
import com.itesm.application.dto.AdminDashboardSummaryDto.AdminDashboardAlertDto;
import com.itesm.application.dto.AdminDashboardSummaryDto.AdminMapZoneDto;
import com.itesm.application.dto.AdminDashboardSummaryDto.AdminMetricCardDto;
import com.itesm.application.dto.AdminDashboardSummaryDto.AdminRecommendedActionDto;
import com.itesm.application.dto.HospitalGeoContextDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.domain.models.Hospital;
import com.itesm.domain.models.HospitalResourceSnapshot;
import com.itesm.domain.models.Outbreak;
import com.itesm.domain.models.OperationalRecommendation;
import com.itesm.domain.repository.HospitalRepository;
import com.itesm.domain.repository.HospitalResourceRepository;
import com.itesm.domain.repository.MunicipalityRepository;
import com.itesm.domain.repository.OperationalRecommendationRepository;
import com.itesm.domain.repository.OutbreakRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class GetAdminDashboardSummaryUseCase {

    @Inject AuthenticatedUserContext authenticatedUserContext;
    @Inject HospitalRepository hospitalRepository;
    @Inject OutbreakRepository outbreakRepository;
    @Inject MunicipalityRepository municipalityRepository;
    @Inject HospitalGeoContextService hospitalGeoContextService;
    @Inject HospitalResourceRepository hospitalResourceRepository;
    @Inject OperationalRecommendationRepository recommendationRepository;
    @Inject OperationalRecommendationDedupeService dedupeService;
    @Inject ObjectMapper objectMapper;

    public AdminDashboardSummaryDto execute() {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        UUID hospitalId = currentUser.getHospitalId();
        if (hospitalId == null) {
            throw new NotFoundException("User has no assigned hospital");
        }

        Hospital hospital = hospitalRepository.findHospitalById(hospitalId)
                .orElseThrow(() -> new NotFoundException("Hospital not found: " + hospitalId));

        HospitalGeoContextDto geoContext = hospitalGeoContextService.resolve(hospital);
        List<Outbreak> outbreaks = outbreakRepository.findActiveByMunicipalityIds(geoContext.getIncludedMunicipalityIds());

        HospitalResourceSnapshot snapshot = hospitalResourceRepository
                .findLatestSnapshotByHospitalId(hospitalId)
                .orElse(null);

        List<OperationalRecommendation> recommendations = recommendationRepository.findByHospitalId(hospitalId)
                .stream()
                .filter(r -> !List.of("COMPLETED", "REJECTED").contains(r.getStatus()))
                .collect(Collectors.toList());
        recommendations = dedupeService.collapseOpenDuplicates(recommendations).stream()
                .limit(5)
                .collect(Collectors.toList());

        AdminDashboardSummaryDto dto = new AdminDashboardSummaryDto();
        dto.setHospitalName(hospital.getName());
        dto.setMunicipalityName(hospital.getMunicipalityName());
        dto.setStateName(hospital.getStateName());
        dto.setGeneratedAt(LocalDateTime.now());
        dto.setTopCards(buildTopCards(hospital, snapshot, outbreaks, recommendations));
        dto.setAlerts(buildAlerts(outbreaks));
        dto.setMapZones(buildMapZones(outbreaks, recommendations));
        dto.setRecommendedActions(buildRecommendedActions(recommendations));
        return dto;
    }

    private List<AdminMetricCardDto> buildTopCards(
            Hospital hospital,
            HospitalResourceSnapshot snapshot,
            List<Outbreak> outbreaks,
            List<OperationalRecommendation> recommendations) {
        List<AdminMetricCardDto> cards = new ArrayList<>();
        OperationalRecommendation topRecommendation = recommendations.stream().findFirst().orElse(null);

        int totalBeds = snapshot != null ? snapshot.getTotalBeds() : (hospital.getBedCount() != null ? hospital.getBedCount() : 0);
        int availBeds = snapshot != null ? snapshot.getAvailableBeds() : 0;
        String bedStatus = availBeds < totalBeds * 0.15 ? "critical" : availBeds < totalBeds * 0.3 ? "warning" : "good";
        AdminMetricCardDto beds = new AdminMetricCardDto("beds", "Available Beds",
                availBeds + "/" + totalBeds, "General Ward", bedStatus, availBeds + " open", "bed");
        beds.setDisplayVariant(bedStatus);
        beds.setProgressPercent(totalBeds > 0 ? Math.max(0, Math.min(100, (int) Math.round(((double) availBeds / totalBeds) * 100))) : 0);
        beds.setProgressColorToken(bedStatus);
        beds.setBadgeTone(bedStatus);
        if (topRecommendation != null) beds.setRecommendedActionId(topRecommendation.getId().toString());
        beds.setActionLabel("Review capacity");
        cards.add(beds);

        int doctors = snapshot != null ? snapshot.getDoctorsOnShift() : (hospital.getDoctorCount() != null ? hospital.getDoctorCount() : 0);
        int nurses = snapshot != null ? snapshot.getNursesOnShift() : (hospital.getNurseCount() != null ? hospital.getNurseCount() : 0);
        AdminMetricCardDto staff = new AdminMetricCardDto("staff", "Staff on Shift",
                (doctors + nurses) + "", "Doctors + Nurses", "good", doctors + " doctors", "users");
        staff.setDisplayVariant("good");
        staff.setProgressPercent(null);
        staff.setProgressColorToken("good");
        staff.setBadgeTone("good");
        if (topRecommendation != null) staff.setRecommendedActionId(topRecommendation.getId().toString());
        staff.setActionLabel("View roster");
        cards.add(staff);

        int icuAvail = snapshot != null ? snapshot.getIcuAvailableBeds() : 0;
        int icuTotal = snapshot != null ? snapshot.getIcuTotalBeds() : 0;
        String icuStatus = icuAvail == 0 ? "critical" : icuAvail <= 2 ? "warning" : "good";
        AdminMetricCardDto icu = new AdminMetricCardDto("icu", "ICU Availability",
                icuAvail + "/" + icuTotal, "ICU Beds", icuStatus, icuAvail + " free", "activity");
        icu.setDisplayVariant(icuStatus);
        icu.setProgressPercent(icuTotal > 0 ? Math.max(0, Math.min(100, (int) Math.round(((double) icuAvail / icuTotal) * 100))) : 0);
        icu.setProgressColorToken(icuStatus);
        icu.setBadgeTone(icuStatus);
        if (topRecommendation != null) icu.setRecommendedActionId(topRecommendation.getId().toString());
        icu.setActionLabel("Open ICU recommendations");
        cards.add(icu);

        long activeOutbreaks = outbreaks.stream().filter(o -> "ACTIVE".equalsIgnoreCase(o.getStatus())).count();
        String outbreakStatus = activeOutbreaks >= 3 ? "critical" : activeOutbreaks >= 1 ? "warning" : "good";
        AdminMetricCardDto outbreakCard = new AdminMetricCardDto("outbreaks", "Active Outbreaks",
                String.valueOf(activeOutbreaks), "Nearby area", outbreakStatus, "nearby", "alert-triangle");
        outbreakCard.setDisplayVariant(outbreakStatus);
        outbreakCard.setProgressPercent((int) Math.min(100, activeOutbreaks * 25));
        outbreakCard.setProgressColorToken(outbreakStatus);
        outbreakCard.setBadgeTone(outbreakStatus);
        if (topRecommendation != null) outbreakCard.setRecommendedActionId(topRecommendation.getId().toString());
        outbreakCard.setActionLabel("Review outbreak map");
        cards.add(outbreakCard);

        return cards;
    }

    private List<AdminDashboardAlertDto> buildAlerts(List<Outbreak> outbreaks) {
        return outbreaks.stream()
                .filter(o -> "ACTIVE".equalsIgnoreCase(o.getStatus()))
                .sorted((a, b) -> Integer.compare(b.getCaseCount(), a.getCaseCount()))
                .limit(10)
                .map(o -> {
                    AdminDashboardAlertDto alert = new AdminDashboardAlertDto();
                    alert.setId(o.getId().toString());
                    alert.setDisease(o.getDisease() != null ? o.getDisease().getName() : "Unknown");
                    alert.setSeverity(o.getCaseCount() > 100 ? "HIGH" : o.getCaseCount() > 30 ? "MEDIUM" : "LOW");
                    alert.setLocation(o.getMunicipality() != null ? o.getMunicipality().getName() : "Unknown");
                    alert.setMessage(o.getCaseCount() + " confirmed cases of " +
                            (o.getDisease() != null ? o.getDisease().getName() : "unknown disease"));
                    alert.setCaseCount(o.getCaseCount());
                    alert.setCreatedAt(o.getStartedAt());
                    return alert;
                })
                .collect(Collectors.toList());
    }

    private List<AdminMapZoneDto> buildMapZones(List<Outbreak> outbreaks, List<OperationalRecommendation> recommendations) {
        String topRecommendationId = recommendations.stream().findFirst().map(r -> r.getId().toString()).orElse(null);
        return outbreaks.stream()
                .filter(o -> o.getMunicipality() != null && o.getMunicipality().getLatitude() != null)
                .collect(Collectors.groupingBy(o -> o.getMunicipality().getId()))
                .entrySet().stream()
                .map(entry -> {
                    var muni = entry.getValue().get(0).getMunicipality();
                    int count = entry.getValue().size();
                    AdminMapZoneDto zone = new AdminMapZoneDto();
                    zone.setMunicipalityId(muni.getId().toString());
                    zone.setMunicipalityName(muni.getName());
                    zone.setOutbreakCount(count);
                    zone.setStatus(count >= 3 ? "critical" : count >= 1 ? "warning" : "active");
                    zone.setDisplayPriorityLabel(count >= 3 ? "Critical priority" : count >= 1 ? "Elevated priority" : "Active");
                    zone.setDisplayColorToken(count >= 3 ? "critical" : count >= 1 ? "warning" : "good");
                    zone.setRecommendedActionId(topRecommendationId);
                    if (muni.getLatitude() != null) zone.setLatitude(muni.getLatitude().doubleValue());
                    if (muni.getLongitude() != null) zone.setLongitude(muni.getLongitude().doubleValue());
                    return zone;
                })
                .collect(Collectors.toList());
    }

    private List<AdminRecommendedActionDto> buildRecommendedActions(List<OperationalRecommendation> recs) {
        return recs.stream()
                .map(r -> {
                    AdminRecommendedActionDto action = new AdminRecommendedActionDto();
                    action.setId(r.getId().toString());
                    action.setTitle(r.getTitle());
                    action.setType(r.getType());
                    action.setSeverity(r.getSeverity());
                    action.setStatus(r.getStatus());
                    action.setTranslations(parseStoredTranslations(r.getContentTranslationsJson()));
                    return action;
                })
                .collect(Collectors.toList());
    }

    private Map<String, com.itesm.application.dto.OperationalRecommendationDto.LocalizedContentDto> parseStoredTranslations(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(rawJson, new TypeReference<Map<String, com.itesm.application.dto.OperationalRecommendationDto.LocalizedContentDto>>() {});
        } catch (Exception ignored) {
            return Map.of();
        }
    }
}
