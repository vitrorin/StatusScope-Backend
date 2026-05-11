package com.itesm.application.usecase;

import com.itesm.application.dto.PatientContextDto;
import com.itesm.domain.models.Disease;
import com.itesm.domain.models.Outbreak;
import com.itesm.domain.models.State;
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

    private State state(String name) {
        State state = new State();
        state.setId(UUID.randomUUID());
        state.setName(name);
        state.setCode("STATE");
        return state;
    }

    private Outbreak outbreak(String diseaseName, String symptoms, int caseCount) {
        Disease d = new Disease();
        d.setId(UUID.randomUUID());
        d.setName(diseaseName);
        d.setSymptoms(symptoms);

        Outbreak o = new Outbreak();
        o.setId(UUID.randomUUID());
        o.setDisease(d);
        o.setScope("MUNICIPALITY");
        o.setCaseCount(caseCount);
        o.setConfirmationStatus("CONFIRMED");
        o.setStartedAt(LocalDateTime.now().minusDays(3));
        o.setStatus("ACTIVE");
        return o;
    }

    @Test
    void shouldContainDiseaseNameWhenOutbreakPresent() {
        String prompt = builder.build(state("Nuevo Leon"), List.of(outbreak("Measles", "Fever, rash", 12)), null);
        Assertions.assertTrue(prompt.contains("Measles"));
        Assertions.assertTrue(prompt.contains("12"));
        Assertions.assertTrue(prompt.contains("CONFIRMED"));
        Assertions.assertTrue(prompt.contains("Fever, rash"));
    }

    @Test
    void shouldOmitOutbreakBlockWhenListEmpty() {
        String prompt = builder.build(state("Nuevo Leon"), List.of(), null);
        Assertions.assertFalse(prompt.contains("Active outbreaks"));
        Assertions.assertFalse(prompt.contains("overlap with an active outbreak"));
    }

    @Test
    void shouldIncludePatientContextWhenProvided() {
        PatientContextDto pc = new PatientContextDto();
        pc.setAgeYears(35);
        pc.setSex("female");
        pc.setSymptoms("fever, rash");

        String prompt = builder.build(state("Nuevo Leon"), List.of(), pc);
        Assertions.assertTrue(prompt.contains("35"));
        Assertions.assertTrue(prompt.contains("female"));
        Assertions.assertTrue(prompt.contains("fever, rash"));
    }

    @Test
    void shouldContainStateNameInPrompt() {
        String prompt = builder.build(state("Ciudad de Mexico"), List.of(), null);
        Assertions.assertTrue(prompt.contains("Ciudad de Mexico"));
    }

    @Test
    void shouldNotLeakOutbreaksFromOtherStates() {
        Outbreak o = outbreak("Dengue", "Fever, joint pain", 5);
        String prompt = builder.build(state("Nuevo Leon"), List.of(o), null);
        Assertions.assertTrue(prompt.contains("Dengue"));
        Assertions.assertFalse(prompt.contains("COVID"));
    }

    @Test
    void shouldIncludeHistoricalCasesWhenProvided() {
        HistoricalCaseRetriever.HistoricalCase hc = new HistoricalCaseRetriever.HistoricalCase(
                UUID.randomUUID(),
                LocalDateTime.now().minusWeeks(2),
                28,
                "female",
                "fever, headache, joint pain",
                "Dengue",
                0.5
        );
        String prompt = builder.build(state("Nuevo Leon"), List.of(), null, List.of(hc));
        Assertions.assertTrue(prompt.contains("Similar confirmed cases"));
        Assertions.assertTrue(prompt.contains("Dengue"));
    }

    @Test
    void shouldInstructLlmToEmitDiagnosisJsonBlock() {
        String prompt = builder.build(state("Nuevo Leon"), List.of(), null, List.of());
        Assertions.assertTrue(prompt.contains("<DIAGNOSIS_JSON>"));
        Assertions.assertTrue(prompt.contains("differentialDiagnoses"));
    }
}
