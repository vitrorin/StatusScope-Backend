package com.itesm.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itesm.application.port.out.AssistantChatGateway;
import com.itesm.application.port.out.AssistantChatMessage;
import com.itesm.domain.models.OperationalRecommendation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class OperationalRecommendationNarrativeComposer {

    private static final Logger LOG = Logger.getLogger(OperationalRecommendationNarrativeComposer.class);
    private static final String MODEL_PROVIDER = "llm-assisted-shared-gateway";
    private static final String MODEL_VERSION = "admin-operational-narrative-v1";

    @ConfigProperty(name = "statusscope.admin.recommendations.llm.enabled", defaultValue = "true")
    boolean enabled;

    @Inject AssistantChatGateway assistantChatGateway;
    @Inject ObjectMapper objectMapper;
    @Inject OperationalRecommendationNarrativePromptBuilder promptBuilder;

    public OperationalRecommendation enhance(OperationalRecommendation recommendation) {
        if (!enabled || recommendation == null) {
            return recommendation;
        }

        try {
            String reply = assistantChatGateway.chat(List.of(
                    new AssistantChatMessage("system", promptBuilder.build(recommendation)),
                    new AssistantChatMessage("user", "Generate a hospital-admin recommendation grounded in this epidemiological and resource context.")
            ));
            NarrativeResponse narrative = parseResponse(reply);
            applyNarrative(recommendation, narrative);
            recommendation.setModelProvider(MODEL_PROVIDER);
            recommendation.setModelVersion(MODEL_VERSION);
            recommendation.setCreatedByMode("LLM_ASSISTED");
            recommendation.setUpdatedAt(LocalDateTime.now());
        } catch (Exception e) {
            LOG.debugf("Skipping LLM recommendation narrative enhancement for '%s': %s",
                    recommendation.getTitle(), e.getMessage());
        }

        return recommendation;
    }

    private NarrativeResponse parseResponse(String rawReply) throws Exception {
        String json = extractJson(rawReply);
        JsonNode node = objectMapper.readTree(json);

        NarrativeResponse response = new NarrativeResponse();
        response.title = text(node, "title");
        response.description = text(node, "description");
        response.expectedImpact = text(node, "expectedImpact");
        response.urgencyWindow = text(node, "urgencyWindow");
        response.rationale = strings(node.get("rationale"));
        response.recommendedActions = strings(node.get("recommendedActions"));
        return response;
    }

    private String extractJson(String rawReply) {
        if (rawReply == null) {
            throw new IllegalArgumentException("LLM reply was empty");
        }
        String trimmed = rawReply.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("LLM reply did not contain a JSON object");
        }
        return trimmed.substring(start, end + 1);
    }

    private void applyNarrative(OperationalRecommendation recommendation, NarrativeResponse response) throws Exception {
        if (response.title != null && !response.title.isBlank()) {
            recommendation.setTitle(response.title.trim());
        }
        if (response.description != null && !response.description.isBlank()) {
            recommendation.setDescription(response.description.trim());
        }
        if (response.expectedImpact != null && !response.expectedImpact.isBlank()) {
            recommendation.setExpectedImpact(response.expectedImpact.trim());
        }
        if (response.urgencyWindow != null && !response.urgencyWindow.isBlank()) {
            recommendation.setUrgencyWindow(response.urgencyWindow.trim());
        }
        if (!response.rationale.isEmpty()) {
            recommendation.setRationaleJson(objectMapper.writeValueAsString(response.rationale));
        }
        if (!response.recommendedActions.isEmpty()) {
            recommendation.setRecommendedActionsJson(objectMapper.writeValueAsString(response.recommendedActions));
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private List<String> strings(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (item != null && !item.isNull()) {
                String value = item.asText();
                if (!value.isBlank()) {
                    values.add(value.trim());
                }
            }
        }
        return values;
    }

    private static class NarrativeResponse {
        private String title;
        private String description;
        private String expectedImpact;
        private String urgencyWindow;
        private List<String> rationale = List.of();
        private List<String> recommendedActions = List.of();
    }
}
