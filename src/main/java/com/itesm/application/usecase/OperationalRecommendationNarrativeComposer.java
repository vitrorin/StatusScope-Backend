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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        JsonNode translations = node.get("translations");
        if (translations != null && translations.isObject()) {
            response.en = localized(translations.get("en"));
            response.es = localized(translations.get("es"));
        } else {
            response.en = new LocalizedNarrative();
            response.en.description = text(node, "description");
            response.en.expectedImpact = text(node, "expectedImpact");
            response.en.urgencyWindow = text(node, "urgencyWindow");
            response.en.rationale = strings(node.get("rationale"));
            response.en.recommendedActions = strings(node.get("recommendedActions"));
        }
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
        LocalizedNarrative en = response.en;
        if (en == null) {
            return;
        }

        if (isEpidemiologyRecommendation(recommendation) && en.title != null && !en.title.isBlank()) {
            recommendation.setTitle(en.title.trim());
        }
        if (en.description != null && !en.description.isBlank()) {
            recommendation.setDescription(en.description.trim());
        }
        if (en.expectedImpact != null && !en.expectedImpact.isBlank()) {
            recommendation.setExpectedImpact(en.expectedImpact.trim());
        }
        if (en.urgencyWindow != null && !en.urgencyWindow.isBlank()) {
            recommendation.setUrgencyWindow(en.urgencyWindow.trim());
        }
        if (!en.rationale.isEmpty()) {
            recommendation.setRationaleJson(objectMapper.writeValueAsString(en.rationale));
        }
        if (!en.recommendedActions.isEmpty()) {
            recommendation.setRecommendedActionsJson(objectMapper.writeValueAsString(en.recommendedActions));
        }

        if (response.hasTranslations()) {
            recommendation.setContentTranslationsJson(objectMapper.writeValueAsString(buildTranslations(recommendation, response)));
        }
    }

    private LocalizedNarrative localized(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        LocalizedNarrative localized = new LocalizedNarrative();
        localized.title = text(node, "title");
        localized.description = text(node, "description");
        localized.expectedImpact = text(node, "expectedImpact");
        localized.urgencyWindow = text(node, "urgencyWindow");
        localized.rationale = strings(node.get("rationale"));
        localized.recommendedActions = strings(node.get("recommendedActions"));
        return localized;
    }

    private Map<String, Map<String, Object>> buildTranslations(
            OperationalRecommendation recommendation,
            NarrativeResponse response) {
        Map<String, Map<String, Object>> translations = new LinkedHashMap<>();
        translations.put("en", contentMap(
                recommendation.getTitle(),
                recommendation.getDescription(),
                recommendation.getExpectedImpact(),
                recommendation.getUrgencyWindow(),
                response.en.rationale,
                response.en.recommendedActions));
        if (response.es != null) {
            translations.put("es", contentMap(
                    valueOrFallback(response.es.title, recommendation.getTitle()),
                    valueOrFallback(response.es.description, recommendation.getDescription()),
                    valueOrFallback(response.es.expectedImpact, recommendation.getExpectedImpact()),
                    valueOrFallback(response.es.urgencyWindow, recommendation.getUrgencyWindow()),
                    response.es.rationale.isEmpty() ? response.en.rationale : response.es.rationale,
                    response.es.recommendedActions.isEmpty() ? response.en.recommendedActions : response.es.recommendedActions));
        }
        return translations;
    }

    private Map<String, Object> contentMap(
            String title,
            String description,
            String expectedImpact,
            String urgencyWindow,
            List<String> rationale,
            List<String> recommendedActions) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("title", title);
        content.put("description", description);
        content.put("expectedImpact", expectedImpact);
        content.put("urgencyWindow", urgencyWindow);
        content.put("rationale", rationale);
        content.put("recommendedActions", recommendedActions);
        return content;
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private boolean isEpidemiologyRecommendation(OperationalRecommendation recommendation) {
        String type = recommendation.getType();
        return type != null && type.toUpperCase().startsWith("EPIDEMIOLOGY");
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
        private LocalizedNarrative en;
        private LocalizedNarrative es;

        private boolean hasTranslations() {
            return es != null;
        }
    }

    private static class LocalizedNarrative {
        private String title;
        private String description;
        private String expectedImpact;
        private String urgencyWindow;
        private List<String> rationale = List.of();
        private List<String> recommendedActions = List.of();
    }
}
