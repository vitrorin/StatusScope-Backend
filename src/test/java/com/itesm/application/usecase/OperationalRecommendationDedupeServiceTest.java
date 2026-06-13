package com.itesm.application.usecase;

import com.itesm.domain.models.OperationalRecommendation;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationalRecommendationDedupeServiceTest {

    private static final UUID HOSPITAL_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

    private final OperationalRecommendationDedupeService service = new OperationalRecommendationDedupeService();

    @Test
    void shouldCollapseLlmTitleVariantsForSameOperationalRule() {
        OperationalRecommendation supply = recommendation(
                "SUPPLY",
                "Urgent Replenishment of Protective and Respiratory Supplies",
                "Immediate action required to replenish critical protective and respiratory supplies.",
                "NEW",
                LocalDateTime.now().minusMinutes(2));
        OperationalRecommendation supplyVariant = recommendation(
                "SUPPLY",
                "Urgently Replenish Critical Protective and Respiratory Supplies",
                "Immediate action is required to replenish critical protective and respiratory supplies.",
                "NEW",
                LocalDateTime.now().minusMinutes(1));

        List<OperationalRecommendation> result = service.collapseOpenDuplicates(List.of(supply, supplyVariant));

        assertEquals(1, result.size());
        assertEquals(supplyVariant.getTitle(), result.get(0).getTitle());
    }

    @Test
    void shouldKeepDistinctBedCapacityRulesSeparate() {
        OperationalRecommendation expandBeds = recommendation(
                "BED_CAPACITY",
                "Urgently Expand Monitored Bed Capacity",
                "Current bed occupancy is critically high at 90%.",
                "NEW",
                LocalDateTime.now().minusMinutes(2));
        OperationalRecommendation icuSurge = recommendation(
                "BED_CAPACITY",
                "Activate ICU Surge Protocol",
                "ICU occupancy is critically high with only 3 beds available.",
                "NEW",
                LocalDateTime.now().minusMinutes(1));

        List<OperationalRecommendation> result = service.collapseOpenDuplicates(List.of(expandBeds, icuSurge));

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(r -> "Urgently Expand Monitored Bed Capacity".equals(r.getTitle())));
        assertTrue(result.stream().anyMatch(r -> "Activate ICU Surge Protocol".equals(r.getTitle())));
    }

    @Test
    void shouldKeepEpidemiologyScopesSeparateForSameDisease() {
        OperationalRecommendation hospital = recommendation(
                "EPIDEMIOLOGY_HOSPITAL",
                "Hospital epidemiological signal - COVID-19",
                "Hospital signal for COVID-19.",
                "NEW",
                LocalDateTime.now().minusMinutes(3));
        hospital.setInputContextJson("""
                {"selectedDisease":{"scope":"HOSPITAL","diseaseName":"COVID-19"}}
                """);
        OperationalRecommendation municipal = recommendation(
                "EPIDEMIOLOGY_MUNICIPAL",
                "Municipal epidemiological signal - COVID-19",
                "Municipal signal for COVID-19.",
                "NEW",
                LocalDateTime.now().minusMinutes(2));
        municipal.setInputContextJson("""
                {"selectedDisease":{"scope":"MUNICIPAL","diseaseName":"COVID-19"}}
                """);

        List<OperationalRecommendation> result = service.collapseOpenDuplicates(List.of(hospital, municipal));

        assertEquals(2, result.size());
    }

    @Test
    void shouldCollapseSameEpidemiologyScopeAndDisease() {
        OperationalRecommendation older = recommendation(
                "EPIDEMIOLOGY_MUNICIPAL",
                "Municipal epidemiological signal - Measles",
                "Municipal signal for Measles.",
                "NEW",
                LocalDateTime.now().minusMinutes(4));
        older.setInputContextJson("""
                {"selectedDisease":{"scope":"MUNICIPAL","diseaseName":"Measles"}}
                """);
        OperationalRecommendation newer = recommendation(
                "EPIDEMIOLOGY_MUNICIPAL",
                "Measles readiness in surrounding municipalities",
                "Updated municipal signal for Measles.",
                "NEW",
                LocalDateTime.now().minusMinutes(1));
        newer.setInputContextJson("""
                {"selectedDisease":{"scope":"MUNICIPAL","diseaseName":"Measles"}}
                """);

        List<OperationalRecommendation> result = service.collapseOpenDuplicates(List.of(older, newer));

        assertEquals(1, result.size());
        assertEquals(newer.getTitle(), result.get(0).getTitle());
    }

    private OperationalRecommendation recommendation(
            String type,
            String title,
            String description,
            String status,
            LocalDateTime updatedAt) {
        OperationalRecommendation recommendation = new OperationalRecommendation();
        recommendation.setId(UUID.randomUUID());
        recommendation.setHospitalId(HOSPITAL_ID);
        recommendation.setType(type);
        recommendation.setCategory(type);
        recommendation.setTitle(title);
        recommendation.setDescription(description);
        recommendation.setExpectedImpact(description);
        recommendation.setStatus(status);
        recommendation.setCreatedAt(updatedAt.minusMinutes(5));
        recommendation.setUpdatedAt(updatedAt);
        return recommendation;
    }
}
