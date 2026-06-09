package com.itesm.application.usecase;

import com.itesm.application.dto.SystemDashboardSummaryDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.domain.models.Hospital;
import com.itesm.domain.models.Outbreak;
import com.itesm.domain.models.User;
import com.itesm.domain.models.UserStatus;
import com.itesm.domain.repository.HospitalRepository;
import com.itesm.domain.repository.OutbreakRepository;
import com.itesm.domain.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class GetSystemDashboardSummaryUseCase {

    private static final double NEARBY_OUTBREAK_RADIUS_KM = 35.0;

    @Inject AuthenticatedUserContext authenticatedUserContext;
    @Inject HospitalRepository hospitalRepository;
    @Inject UserRepository userRepository;
    @Inject OutbreakRepository outbreakRepository;

    @ConfigProperty(name = "statusscope.admin.recommendations.llm.enabled", defaultValue = "true")
    boolean llmEnabled;

    @ConfigProperty(name = "openai.api-key", defaultValue = "")
    String openAiApiKey;

    @ConfigProperty(name = "gemini.api-key", defaultValue = "")
    String geminiApiKey;

    public SystemDashboardSummaryDto execute() {
        if (!authenticatedUserContext.getCurrentUser().isSystemAdmin()) {
            throw new ForbiddenException("System dashboard not available");
        }

        List<Hospital> hospitals = hospitalRepository.listAllDomain();
        List<User> users = userRepository.listAllDomain();
        long activeHospitals = hospitals.stream().filter(Hospital::isActive).count();
        long activeUsers = users.stream().filter(user -> user.getStatus() == UserStatus.ACTIVE).count();
        boolean aiConfigured = llmEnabled && (!isBlank(openAiApiKey) || !isBlank(geminiApiKey));

        SystemDashboardSummaryDto dto = new SystemDashboardSummaryDto();
        dto.setGeneratedAt(LocalDateTime.now());
        dto.setMetrics(List.of(
                new SystemDashboardSummaryDto.SystemMetricDto(
                        "hospitals",
                        "Total Registered Hospitals",
                        String.valueOf(hospitals.size()),
                        activeHospitals + " active partners",
                        "good",
                        "hospital"),
                new SystemDashboardSummaryDto.SystemMetricDto(
                        "users",
                        "Active Users",
                        String.valueOf(activeUsers),
                        users.size() + " total platform users",
                        activeUsers > 0 ? "good" : "warning",
                        "users"),
                new SystemDashboardSummaryDto.SystemMetricDto(
                        "system",
                        "System Status",
                        "Operational",
                        "Database and API available",
                        "good",
                        "check"),
                new SystemDashboardSummaryDto.SystemMetricDto(
                        "ai",
                        "AI Services Status",
                        aiConfigured ? "Running" : "Needs config",
                        aiConfigured ? "LLM provider configured" : "Missing provider key",
                        aiConfigured ? "good" : "warning",
                        "cpu")
        ));
        dto.setUserActivity(buildActivity(users));
        dto.setRegionalDistribution(buildRegionalDistribution(hospitals));
        dto.setRecentEvents(buildEvents(users, hospitals));
        dto.setHospitalOutbreaks(buildHospitalOutbreaks(hospitals, outbreakRepository.findAllActiveMunicipal()));
        dto.setHospitalUserMetrics(buildHospitalUserMetrics(hospitals, users));
        return dto;
    }

    private List<SystemDashboardSummaryDto.SystemActivityPointDto> buildActivity(List<User> users) {
        LocalDate today = LocalDate.now();
        Map<LocalDate, List<User>> loginsByDate = users.stream()
                .filter(user -> user.getLastLoginAt() != null)
                .collect(Collectors.groupingBy(user -> user.getLastLoginAt().toLocalDate()));

        return Stream.iterate(today.minusDays(6), date -> date.plusDays(1))
                .limit(7)
                .map(date -> {
                    List<User> dayUsers = loginsByDate.getOrDefault(date, List.of());
                    int adminValue = (int) dayUsers.stream().filter(this::isHospitalAdminUser).count();
                    int doctorValue = (int) dayUsers.stream().filter(this::isDoctorUser).count();
                    int total = Math.max(dayUsers.size(), adminValue + doctorValue);
                    return new SystemDashboardSummaryDto.SystemActivityPointDto(
                            date.toString(),
                            date,
                            total,
                            adminValue,
                            doctorValue);
                })
                .collect(Collectors.toList());
    }

    private boolean isHospitalAdminUser(User user) {
        return user.getRoles().stream().anyMatch(role -> {
            String code = role.getCode();
            return "HOSPITAL_ADMIN".equals(code);
        });
    }

    private boolean isDoctorUser(User user) {
        return user.getRoles().stream().anyMatch(role -> "DOCTOR".equals(role.getCode()));
    }

    private List<SystemDashboardSummaryDto.SystemRegionalDistributionDto> buildRegionalDistribution(List<Hospital> hospitals) {
        int total = Math.max(1, hospitals.size());
        return hospitals.stream()
                .collect(Collectors.groupingBy(h -> {
                    if (!isBlank(h.getStateName())) return h.getStateName();
                    if (!isBlank(h.getMunicipalityName())) return h.getMunicipalityName();
                    return "Unassigned";
                }, Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(entry -> new SystemDashboardSummaryDto.SystemRegionalDistributionDto(
                        entry.getKey(),
                        entry.getValue().intValue(),
                        (int) Math.round((entry.getValue() * 100.0) / total)))
                .collect(Collectors.toList());
    }

    private List<SystemDashboardSummaryDto.SystemEventDto> buildEvents(List<User> users, List<Hospital> hospitals) {
        Stream<SystemDashboardSummaryDto.SystemEventDto> loginEvents = users.stream()
                .filter(user -> user.getLastLoginAt() != null)
                .map(user -> new SystemDashboardSummaryDto.SystemEventDto(
                        user.getId().toString() + "-login",
                        "Successful login: " + user.getFullName(),
                        user.getEmail(),
                        "login",
                        user.getLastLoginAt()));

        Stream<SystemDashboardSummaryDto.SystemEventDto> hospitalEvents = hospitals.stream()
                .filter(hospital -> hospital.getCreatedAt() != null)
                .map(hospital -> new SystemDashboardSummaryDto.SystemEventDto(
                        hospital.getId().toString() + "-hospital",
                        "Hospital registered: " + hospital.getName(),
                        hospital.getMunicipalityName() != null ? hospital.getMunicipalityName() : hospital.getCode(),
                        hospital.isActive() ? "hospital" : "warning",
                        hospital.getCreatedAt()));

        return Stream.concat(loginEvents, hospitalEvents)
                .sorted(Comparator.comparing(SystemDashboardSummaryDto.SystemEventDto::getOccurredAt).reversed())
                .limit(6)
                .collect(Collectors.toList());
    }

    private List<SystemDashboardSummaryDto.SystemHospitalOutbreakDto> buildHospitalOutbreaks(
            List<Hospital> hospitals,
            List<Outbreak> activeMunicipalOutbreaks) {
        return hospitals.stream()
                .sorted(Comparator.comparing(Hospital::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(hospital -> {
                    List<NearbyOutbreakCandidate> nearby = activeMunicipalOutbreaks.stream()
                            .map(outbreak -> toNearbyCandidate(hospital, outbreak))
                            .filter(Objects::nonNull)
                            .filter(candidate -> candidate.distanceKm() <= NEARBY_OUTBREAK_RADIUS_KM)
                            .sorted(Comparator
                                    .comparingInt((NearbyOutbreakCandidate candidate) -> severityRank(candidate.severity())).reversed()
                                    .thenComparing((NearbyOutbreakCandidate candidate) -> candidate.outbreak().getCaseCount(), Comparator.reverseOrder())
                                    .thenComparingDouble(NearbyOutbreakCandidate::distanceKm))
                            .collect(Collectors.toList());

                    List<SystemDashboardSummaryDto.SystemNearbyOutbreakDto> topOutbreaks = nearby.stream()
                            .limit(5)
                            .map(this::toNearbyOutbreakDto)
                            .collect(Collectors.toList());

                    return new SystemDashboardSummaryDto.SystemHospitalOutbreakDto(
                            hospital.getId() == null ? null : hospital.getId().toString(),
                            hospital.getCode(),
                            hospital.getName(),
                            hospital.getMunicipalityName(),
                            hospital.getStateName(),
                            hospital.isActive(),
                            toDouble(hospital.getLatitude()),
                            toDouble(hospital.getLongitude()),
                            nearby.size(),
                            NEARBY_OUTBREAK_RADIUS_KM,
                            topOutbreaks);
                })
                .collect(Collectors.toList());
    }

    private List<SystemDashboardSummaryDto.SystemHospitalUserMetricDto> buildHospitalUserMetrics(
            List<Hospital> hospitals,
            List<User> users) {
        return hospitals.stream()
                .map(hospital -> {
                    List<User> hospitalUsers = users.stream()
                            .filter(user -> hospital.getId() != null && hospital.getId().equals(user.getHospitalId()))
                            .collect(Collectors.toList());
                    int adminUsers = (int) hospitalUsers.stream().filter(this::isHospitalAdminUser).count();
                    int doctorUsers = (int) hospitalUsers.stream().filter(this::isDoctorUser).count();
                    int activeUsers = (int) hospitalUsers.stream().filter(user -> user.getStatus() == UserStatus.ACTIVE).count();
                    return new SystemDashboardSummaryDto.SystemHospitalUserMetricDto(
                            hospital.getId() == null ? null : hospital.getId().toString(),
                            hospital.getName(),
                            hospital.getMunicipalityName(),
                            hospital.getStateName(),
                            hospitalUsers.size(),
                            activeUsers,
                            adminUsers,
                            doctorUsers,
                            hospitalUsers.size() - activeUsers);
                })
                .sorted(Comparator.comparingInt(SystemDashboardSummaryDto.SystemHospitalUserMetricDto::getTotalUsers).reversed())
                .collect(Collectors.toList());
    }

    private NearbyOutbreakCandidate toNearbyCandidate(Hospital hospital, Outbreak outbreak) {
        if (hospital == null || outbreak == null || outbreak.getMunicipality() == null) {
            return null;
        }
        Double hospitalLat = toDouble(hospital.getLatitude());
        Double hospitalLon = toDouble(hospital.getLongitude());
        Double outbreakLat = toDouble(outbreak.getMunicipality().getLatitude());
        Double outbreakLon = toDouble(outbreak.getMunicipality().getLongitude());
        if (hospitalLat == null || hospitalLon == null || outbreakLat == null || outbreakLon == null) {
            return null;
        }
        double distanceKm = haversineKm(hospitalLat, hospitalLon, outbreakLat, outbreakLon);
        return new NearbyOutbreakCandidate(outbreak, distanceKm, outbreakSeverity(outbreak));
    }

    private SystemDashboardSummaryDto.SystemNearbyOutbreakDto toNearbyOutbreakDto(NearbyOutbreakCandidate candidate) {
        Outbreak outbreak = candidate.outbreak();
        return new SystemDashboardSummaryDto.SystemNearbyOutbreakDto(
                outbreak.getId() == null ? null : outbreak.getId().toString(),
                outbreak.getDisease() == null ? "Unknown disease" : outbreak.getDisease().getName(),
                outbreak.getMunicipality() == null ? null : outbreak.getMunicipality().getName(),
                outbreak.getMunicipality() == null ? null : outbreak.getMunicipality().getStateName(),
                outbreak.getCaseCount(),
                outbreak.getConfirmationStatus(),
                candidate.severity(),
                Math.round(candidate.distanceKm() * 10.0) / 10.0,
                outbreak.getStartedAt());
    }

    private String outbreakSeverity(Outbreak outbreak) {
        int caseCount = outbreak.getCaseCount();
        if (caseCount >= 75) return "CRITICAL";
        if (caseCount >= 35) return "HIGH";
        if (caseCount >= 12) return "MEDIUM";
        return "LOW";
    }

    private int severityRank(String severity) {
        if ("CRITICAL".equalsIgnoreCase(severity)) return 4;
        if ("HIGH".equalsIgnoreCase(severity)) return 3;
        if ("MEDIUM".equalsIgnoreCase(severity)) return 2;
        return 1;
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record NearbyOutbreakCandidate(Outbreak outbreak, double distanceKm, String severity) {}
}
