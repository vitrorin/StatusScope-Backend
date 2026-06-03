package com.itesm.application.usecase;

import com.itesm.application.dto.SystemDashboardSummaryDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.domain.models.Hospital;
import com.itesm.domain.models.User;
import com.itesm.domain.models.UserStatus;
import com.itesm.domain.repository.HospitalRepository;
import com.itesm.domain.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class GetSystemDashboardSummaryUseCase {

    @Inject AuthenticatedUserContext authenticatedUserContext;
    @Inject HospitalRepository hospitalRepository;
    @Inject UserRepository userRepository;

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
        return dto;
    }

    private List<SystemDashboardSummaryDto.SystemActivityPointDto> buildActivity(List<User> users) {
        LocalDate today = LocalDate.now();
        Map<LocalDate, Long> loginsByDate = users.stream()
                .map(User::getLastLoginAt)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(LocalDateTime::toLocalDate, Collectors.counting()));

        return Stream.iterate(today.minusDays(6), date -> date.plusDays(1))
                .limit(7)
                .map(date -> new SystemDashboardSummaryDto.SystemActivityPointDto(
                        labelForDay(date.getDayOfWeek()),
                        loginsByDate.getOrDefault(date, 0L).intValue()))
                .collect(Collectors.toList());
    }

    private String labelForDay(DayOfWeek day) {
        return day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase(Locale.ROOT);
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
