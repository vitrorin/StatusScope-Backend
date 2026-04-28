package com.itesm.application.usecase;

import com.itesm.application.dto.PatientContextDto;
import com.itesm.domain.models.Disease;
import com.itesm.domain.models.Outbreak;
import com.itesm.domain.models.Region;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

class AssistantPromptBuilderTest {

    private AssistantPromptBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new AssistantPromptBuilder();
    }

    private Region region(String name) {
        Region r = new Region();
        r.setId(UUID.randomUUID());
        r.setName(name);
        r.setCode("REG");
        return r;
    }

    private Outbreak outbreak(String diseaseName, String symptoms, int caseCount) {
        Disease d = new Disease();
        d.setId(UUID.randomUUID());
        d.setName(diseaseName);
        d.setSymptoms(symptoms);

        Outbreak o = new Outbreak();
        o.setId(UUID.randomUUID());
        o.setDisease(d);
        o.setCaseCount(caseCount);
        o.setStartedAt(LocalDateTime.now().minusDays(3));
        o.setStatus("ACTIVE");
        return o;
    }

    @Test
    void shouldContainDiseaseNameWhenOutbreakPresent() {
        String prompt = builder.build(region("Región Norte"), List.of(outbreak("Measles", "Fever, rash", 12)), null);
        Assertions.assertTrue(prompt.contains("Measles"));
        Assertions.assertTrue(prompt.contains("12"));
        Assertions.assertTrue(prompt.contains("Fever, rash"));
    }

    @Test
    void shouldOmitOutbreakBlockWhenListEmpty() {
        String prompt = builder.build(region("Región Norte"), List.of(), null);
        Assertions.assertFalse(prompt.contains("Active outbreaks"));
        Assertions.assertFalse(prompt.contains("overlap with an active outbreak"));
    }

    @Test
    void shouldIncludePatientContextWhenProvided() {
        PatientContextDto pc = new PatientContextDto();
        pc.setAgeYears(35);
        pc.setSex("female");
        pc.setPostalCode("64000");
        pc.setSymptoms("fever, rash");

        String prompt = builder.build(region("Región Norte"), List.of(), pc);
        Assertions.assertTrue(prompt.contains("35"));
        Assertions.assertTrue(prompt.contains("female"));
        Assertions.assertTrue(prompt.contains("64000"));
        Assertions.assertTrue(prompt.contains("fever, rash"));
    }

    @Test
    void shouldContainRegionNameInPrompt() {
        String prompt = builder.build(region("Región Centro"), List.of(), null);
        Assertions.assertTrue(prompt.contains("Región Centro"));
    }

    @Test
    void shouldNotLeakOutbreaksFromOtherRegions() {
        // The builder only receives outbreaks already filtered by region — it should render
        // what it gets. This test ensures it doesn't add extra items if called with one outbreak.
        Outbreak o = outbreak("Dengue", "Fever, joint pain", 5);
        String prompt = builder.build(region("Región Norte"), List.of(o), null);
        Assertions.assertTrue(prompt.contains("Dengue"));
        Assertions.assertFalse(prompt.contains("COVID")); // no covid in the list
    }
}
