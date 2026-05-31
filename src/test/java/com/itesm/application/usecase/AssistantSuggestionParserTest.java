package com.itesm.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itesm.application.dto.AssistantSuggestionDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class AssistantSuggestionParserTest {

    private AssistantSuggestionParser parser;

    @BeforeEach
    void setUp() {
        parser = new AssistantSuggestionParser();
        parser.objectMapper = new ObjectMapper();
    }

    // ── Null / empty input ────────────────────────────────────────────────────

    @Test
    void shouldReturnEmptyForNullInput() {
        AssistantSuggestionParser.ParseResult result = parser.parse(null);
        Assertions.assertEquals("", result.cleanedReply());
        Assertions.assertTrue(result.suggestions().isEmpty());
    }

    @Test
    void shouldReturnEmptyForEmptyInput() {
        AssistantSuggestionParser.ParseResult result = parser.parse("");
        Assertions.assertEquals("", result.cleanedReply());
        Assertions.assertTrue(result.suggestions().isEmpty());
    }

    // ── Missing block ─────────────────────────────────────────────────────────

    @Test
    void shouldReturnOriginalReplyWhenNoJsonBlock() {
        String raw = "Based on symptoms, consider influenza.";
        AssistantSuggestionParser.ParseResult result = parser.parse(raw);
        Assertions.assertEquals(raw, result.cleanedReply());
        Assertions.assertTrue(result.suggestions().isEmpty());
    }

    @Test
    void shouldReturnReplyWithoutJsonWhenBlockPresentButMalformed() {
        // The regex requires a JSON object ({}), so a block without curly braces won't match
        // In that case the parser returns the original reply and no suggestions.
        String raw = "Some text <DIAGNOSIS_JSON>this is not json</DIAGNOSIS_JSON> more text";
        AssistantSuggestionParser.ParseResult result = parser.parse(raw);
        Assertions.assertTrue(result.suggestions().isEmpty());
        // When regex doesn't match, the original reply is returned unchanged
        Assertions.assertEquals(raw, result.cleanedReply());
    }

    @Test
    void shouldReturnEmptySuggestionsForMalformedJsonObject() {
        // When the block matches (has curly braces) but JSON is malformed, suggestions are dropped
        String raw = "Some text <DIAGNOSIS_JSON>{not valid json}</DIAGNOSIS_JSON> more text";
        AssistantSuggestionParser.ParseResult result = parser.parse(raw);
        Assertions.assertTrue(result.suggestions().isEmpty());
        Assertions.assertFalse(result.cleanedReply().contains("<DIAGNOSIS_JSON>"));
    }

    // ── Single suggestion ─────────────────────────────────────────────────────

    @Test
    void shouldParseSingleSuggestion() {
        String json = """
                {
                  "differentialDiagnoses": [
                    {
                      "displayName": "Measles",
                      "confidence": 0.85,
                      "rationale": "Fever and Koplik spots",
                      "localityRiskLevel": "HIGH",
                      "isPrimary": true
                    }
                  ]
                }
                """;
        String raw = "Consider the following diagnoses.\n<DIAGNOSIS_JSON>" + json + "</DIAGNOSIS_JSON>";
        AssistantSuggestionParser.ParseResult result = parser.parse(raw);

        Assertions.assertEquals(1, result.suggestions().size());
        AssistantSuggestionDto suggestion = result.suggestions().get(0);
        Assertions.assertEquals("Measles", suggestion.getDisplayName());
        Assertions.assertEquals(1, suggestion.getRankOrder());
        Assertions.assertEquals(0.85, suggestion.getConfidence(), 0.001);
        Assertions.assertEquals("Fever and Koplik spots", suggestion.getRationale());
        Assertions.assertEquals("HIGH", suggestion.getLocalityRiskLevel());
        Assertions.assertTrue(suggestion.isPrimary());
    }

    @Test
    void shouldStripJsonBlockFromCleanedReply() {
        String json = """
                {"differentialDiagnoses": [{"displayName": "Dengue", "isPrimary": true}]}
                """;
        String raw = "Consider dengue.\n<DIAGNOSIS_JSON>" + json + "</DIAGNOSIS_JSON>";
        AssistantSuggestionParser.ParseResult result = parser.parse(raw);

        Assertions.assertFalse(result.cleanedReply().contains("<DIAGNOSIS_JSON>"));
        Assertions.assertFalse(result.cleanedReply().contains("</DIAGNOSIS_JSON>"));
        Assertions.assertTrue(result.cleanedReply().contains("Consider dengue."));
    }

    // ── Multiple suggestions ──────────────────────────────────────────────────

    @Test
    void shouldParseMultipleSuggestions() {
        String json = """
                {
                  "differentialDiagnoses": [
                    {"displayName": "Dengue", "isPrimary": true, "confidence": 0.9},
                    {"displayName": "Malaria", "isPrimary": false, "confidence": 0.6},
                    {"displayName": "Typhoid", "isPrimary": false, "confidence": 0.3}
                  ]
                }
                """;
        String raw = "Differential diagnosis.\n<DIAGNOSIS_JSON>" + json + "</DIAGNOSIS_JSON>";
        AssistantSuggestionParser.ParseResult result = parser.parse(raw);

        Assertions.assertEquals(3, result.suggestions().size());
        Assertions.assertEquals("Dengue", result.suggestions().get(0).getDisplayName());
        Assertions.assertEquals(1, result.suggestions().get(0).getRankOrder());
        Assertions.assertEquals("Malaria", result.suggestions().get(1).getDisplayName());
        Assertions.assertEquals(2, result.suggestions().get(1).getRankOrder());
        Assertions.assertEquals("Typhoid", result.suggestions().get(2).getDisplayName());
        Assertions.assertEquals(3, result.suggestions().get(2).getRankOrder());
    }

    @Test
    void firstSuggestionShouldBePrimaryByDefault() {
        String json = """
                {
                  "differentialDiagnoses": [
                    {"displayName": "Flu"},
                    {"displayName": "Cold"}
                  ]
                }
                """;
        String raw = "<DIAGNOSIS_JSON>" + json + "</DIAGNOSIS_JSON>";
        AssistantSuggestionParser.ParseResult result = parser.parse(raw);

        Assertions.assertTrue(result.suggestions().get(0).isPrimary());
        Assertions.assertFalse(result.suggestions().get(1).isPrimary());
    }

    // ── Rank ordering ─────────────────────────────────────────────────────────

    @Test
    void rankOrderShouldStartAtOne() {
        String json = """
                {"differentialDiagnoses": [{"displayName": "Influenza"}]}
                """;
        String raw = "<DIAGNOSIS_JSON>" + json + "</DIAGNOSIS_JSON>";
        AssistantSuggestionParser.ParseResult result = parser.parse(raw);

        Assertions.assertEquals(1, result.suggestions().get(0).getRankOrder());
    }

    // ── Confidence clamping ───────────────────────────────────────────────────

    @Test
    void shouldClampConfidenceAboveOneToOne() {
        String json = """
                {"differentialDiagnoses": [{"displayName": "X", "confidence": 1.5}]}
                """;
        String raw = "<DIAGNOSIS_JSON>" + json + "</DIAGNOSIS_JSON>";
        AssistantSuggestionParser.ParseResult result = parser.parse(raw);

        Assertions.assertEquals(1.0, result.suggestions().get(0).getConfidence(), 0.001);
    }

    @Test
    void shouldClampNegativeConfidenceToZero() {
        String json = """
                {"differentialDiagnoses": [{"displayName": "X", "confidence": -0.5}]}
                """;
        String raw = "<DIAGNOSIS_JSON>" + json + "</DIAGNOSIS_JSON>";
        AssistantSuggestionParser.ParseResult result = parser.parse(raw);

        Assertions.assertEquals(0.0, result.suggestions().get(0).getConfidence(), 0.001);
    }

    @Test
    void shouldReturnNullConfidenceWhenAbsent() {
        String json = """
                {"differentialDiagnoses": [{"displayName": "Flu"}]}
                """;
        String raw = "<DIAGNOSIS_JSON>" + json + "</DIAGNOSIS_JSON>";
        AssistantSuggestionParser.ParseResult result = parser.parse(raw);

        Assertions.assertNull(result.suggestions().get(0).getConfidence());
    }

    @Test
    void shouldParseStringConfidence() {
        String json = """
                {"differentialDiagnoses": [{"displayName": "Flu", "confidence": "0.75"}]}
                """;
        String raw = "<DIAGNOSIS_JSON>" + json + "</DIAGNOSIS_JSON>";
        AssistantSuggestionParser.ParseResult result = parser.parse(raw);

        Assertions.assertEquals(0.75, result.suggestions().get(0).getConfidence(), 0.001);
    }

    @Test
    void shouldReturnNullConfidenceForInvalidString() {
        String json = """
                {"differentialDiagnoses": [{"displayName": "Flu", "confidence": "notanumber"}]}
                """;
        String raw = "<DIAGNOSIS_JSON>" + json + "</DIAGNOSIS_JSON>";
        AssistantSuggestionParser.ParseResult result = parser.parse(raw);

        Assertions.assertNull(result.suggestions().get(0).getConfidence());
    }

    // ── Locality risk normalization ───────────────────────────────────────────

    @Test
    void shouldNormalizeLocalityRiskToUpperCase() {
        String json = """
                {"differentialDiagnoses": [{"displayName": "D", "localityRiskLevel": "medium"}]}
                """;
        String raw = "<DIAGNOSIS_JSON>" + json + "</DIAGNOSIS_JSON>";
        AssistantSuggestionParser.ParseResult result = parser.parse(raw);

        Assertions.assertEquals("MEDIUM", result.suggestions().get(0).getLocalityRiskLevel());
    }

    @Test
    void shouldReturnNullForInvalidLocalityRisk() {
        String json = """
                {"differentialDiagnoses": [{"displayName": "D", "localityRiskLevel": "CRITICAL"}]}
                """;
        String raw = "<DIAGNOSIS_JSON>" + json + "</DIAGNOSIS_JSON>";
        AssistantSuggestionParser.ParseResult result = parser.parse(raw);

        Assertions.assertNull(result.suggestions().get(0).getLocalityRiskLevel());
    }

    @Test
    void shouldAcceptAllValidRiskLevels() {
        for (String risk : List.of("HIGH", "MEDIUM", "LOW", "NONE")) {
            String json = String.format(
                    "{\"differentialDiagnoses\": [{\"displayName\": \"D\", \"localityRiskLevel\": \"%s\"}]}",
                    risk
            );
            String raw = "<DIAGNOSIS_JSON>" + json + "</DIAGNOSIS_JSON>";
            AssistantSuggestionParser.ParseResult result = parser.parse(raw);
            Assertions.assertEquals(risk, result.suggestions().get(0).getLocalityRiskLevel(),
                    "Expected risk level " + risk);
        }
    }

    // ── Entry without displayName is skipped ──────────────────────────────────

    @Test
    void shouldSkipEntryWithoutDisplayName() {
        String json = """
                {
                  "differentialDiagnoses": [
                    {"confidence": 0.8},
                    {"displayName": "Dengue"}
                  ]
                }
                """;
        String raw = "<DIAGNOSIS_JSON>" + json + "</DIAGNOSIS_JSON>";
        AssistantSuggestionParser.ParseResult result = parser.parse(raw);

        Assertions.assertEquals(1, result.suggestions().size());
        Assertions.assertEquals("Dengue", result.suggestions().get(0).getDisplayName());
    }

    @Test
    void shouldSkipEntryWithEmptyDisplayName() {
        String json = """
                {
                  "differentialDiagnoses": [
                    {"displayName": "   "},
                    {"displayName": "Valid Disease"}
                  ]
                }
                """;
        String raw = "<DIAGNOSIS_JSON>" + json + "</DIAGNOSIS_JSON>";
        AssistantSuggestionParser.ParseResult result = parser.parse(raw);

        Assertions.assertEquals(1, result.suggestions().size());
        Assertions.assertEquals("Valid Disease", result.suggestions().get(0).getDisplayName());
    }

    // ── Case-insensitive tag matching ─────────────────────────────────────────

    @Test
    void shouldMatchCaseInsensitiveTag() {
        String json = """
                {"differentialDiagnoses": [{"displayName": "Flu"}]}
                """;
        String raw = "<diagnosis_json>" + json + "</diagnosis_json>";
        AssistantSuggestionParser.ParseResult result = parser.parse(raw);

        Assertions.assertEquals(1, result.suggestions().size());
        Assertions.assertEquals("Flu", result.suggestions().get(0).getDisplayName());
    }

    // ── Rationale ─────────────────────────────────────────────────────────────

    @Test
    void shouldParseRationale() {
        String json = """
                {
                  "differentialDiagnoses": [
                    {"displayName": "Flu", "rationale": "Classic flu-like presentation"}
                  ]
                }
                """;
        String raw = "<DIAGNOSIS_JSON>" + json + "</DIAGNOSIS_JSON>";
        AssistantSuggestionParser.ParseResult result = parser.parse(raw);

        Assertions.assertEquals("Classic flu-like presentation", result.suggestions().get(0).getRationale());
    }

    @Test
    void shouldReturnNullRationaleWhenAbsent() {
        String json = """
                {"differentialDiagnoses": [{"displayName": "Flu"}]}
                """;
        String raw = "<DIAGNOSIS_JSON>" + json + "</DIAGNOSIS_JSON>";
        AssistantSuggestionParser.ParseResult result = parser.parse(raw);

        Assertions.assertNull(result.suggestions().get(0).getRationale());
    }

    // ── isPrimary override ────────────────────────────────────────────────────

    @Test
    void shouldRespectExplicitIsPrimaryFalseForFirstEntry() {
        String json = """
                {
                  "differentialDiagnoses": [
                    {"displayName": "Flu", "isPrimary": false},
                    {"displayName": "Cold", "isPrimary": true}
                  ]
                }
                """;
        String raw = "<DIAGNOSIS_JSON>" + json + "</DIAGNOSIS_JSON>";
        AssistantSuggestionParser.ParseResult result = parser.parse(raw);

        Assertions.assertFalse(result.suggestions().get(0).isPrimary());
        Assertions.assertTrue(result.suggestions().get(1).isPrimary());
    }

    // ── Empty differentialDiagnoses array ─────────────────────────────────────

    @Test
    void shouldHandleEmptyDiagnosesArray() {
        String json = """
                {"differentialDiagnoses": []}
                """;
        String raw = "Some text\n<DIAGNOSIS_JSON>" + json + "</DIAGNOSIS_JSON>";
        AssistantSuggestionParser.ParseResult result = parser.parse(raw);

        Assertions.assertTrue(result.suggestions().isEmpty());
        Assertions.assertTrue(result.cleanedReply().contains("Some text"));
    }
}
