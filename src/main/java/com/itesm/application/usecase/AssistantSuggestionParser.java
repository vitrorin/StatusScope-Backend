package com.itesm.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itesm.application.dto.AssistantSuggestionDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the structured diagnosis suggestions block emitted by the LLM.
 *
 * The model is instructed to append a single block at the end of its reply:
 *   <DIAGNOSIS_JSON>{"differentialDiagnoses": [...]}</DIAGNOSIS_JSON>
 *
 * If the block is missing or malformed we silently drop it — the textual reply
 * is still returned to the doctor. This keeps the loop resilient to model
 * formatting drift.
 */
@ApplicationScoped
public class AssistantSuggestionParser {

    private static final Logger LOG = Logger.getLogger(AssistantSuggestionParser.class);

    private static final Pattern BLOCK = Pattern.compile(
            "<DIAGNOSIS_JSON>\\s*(\\{[\\s\\S]*?\\})\\s*</DIAGNOSIS_JSON>",
            Pattern.CASE_INSENSITIVE);

    private static final Set<String> ALLOWED_RISK = Set.of("HIGH", "MEDIUM", "LOW", "NONE");

    @Inject
    ObjectMapper objectMapper;

    public ParseResult parse(String rawReply) {
        if (rawReply == null || rawReply.isEmpty()) {
            return new ParseResult("", List.of());
        }

        Matcher matcher = BLOCK.matcher(rawReply);
        if (!matcher.find()) {
            return new ParseResult(rawReply, List.of());
        }

        String json = matcher.group(1);
        String cleaned = (rawReply.substring(0, matcher.start()) + rawReply.substring(matcher.end())).trim();

        List<AssistantSuggestionDto> suggestions = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode list = root.path("differentialDiagnoses");
            if (list.isArray()) {
                int rank = 1;
                for (JsonNode node : list) {
                    AssistantSuggestionDto dto = readNode(node, rank);
                    if (dto != null) {
                        suggestions.add(dto);
                        rank++;
                    }
                }
            }
        } catch (Exception ex) {
            LOG.warnf("Could not parse <DIAGNOSIS_JSON> block: %s", ex.getMessage());
            return new ParseResult(cleaned, List.of());
        }

        return new ParseResult(cleaned, suggestions);
    }

    private AssistantSuggestionDto readNode(JsonNode node, int rank) {
        String displayName = textOrNull(node, "displayName");
        if (displayName == null) {
            return null;
        }

        AssistantSuggestionDto dto = new AssistantSuggestionDto();
        dto.setDisplayName(displayName);
        dto.setRankOrder(rank);
        dto.setConfidence(parseConfidence(node.path("confidence")));
        dto.setRationale(textOrNull(node, "rationale"));
        dto.setLocalityRiskLevel(normalizeRisk(textOrNull(node, "localityRiskLevel")));
        dto.setPrimary(node.path("isPrimary").asBoolean(rank == 1));
        return dto;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText("").trim();
        return text.isEmpty() ? null : text;
    }

    private static Double parseConfidence(JsonNode value) {
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return clamp(value.asDouble());
        }
        try {
            return clamp(Double.parseDouble(value.asText().trim()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Double clamp(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return null;
        if (v < 0) return 0d;
        if (v > 1) return 1d;
        return v;
    }

    private static String normalizeRisk(String value) {
        if (value == null) return null;
        String upper = value.trim().toUpperCase(Locale.ROOT);
        return ALLOWED_RISK.contains(upper) ? upper : null;
    }

    public record ParseResult(String cleanedReply, List<AssistantSuggestionDto> suggestions) {}
}
