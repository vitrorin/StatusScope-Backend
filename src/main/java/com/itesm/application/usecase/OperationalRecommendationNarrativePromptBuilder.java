package com.itesm.application.usecase;

import com.itesm.domain.models.OperationalRecommendation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OperationalRecommendationNarrativePromptBuilder {

    public String build(OperationalRecommendation recommendation) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an operational intelligence assistant for hospital administrators.\n");
        prompt.append("Your primary grounding inputs are live epidemiological conditions and current hospital resource state.\n");
        prompt.append("Produce a real-time recommendation for the hospital admin from that context.\n");
        prompt.append("You may improve wording, priority framing, and action sequencing, but you must stay faithful to the provided facts.\n");
        prompt.append("Do not invent staffing counts, bed counts, supply quantities, outbreak statistics, or hospital resources not present in the context.\n");
        prompt.append("Return JSON only in this exact shape:\n");
        prompt.append("{");
        prompt.append("\"translations\":{");
        prompt.append("\"en\":{\"title\":\"...\",\"description\":\"...\",\"expectedImpact\":\"...\",\"urgencyWindow\":\"...\",\"rationale\":[\"...\"],\"recommendedActions\":[\"...\"]},");
        prompt.append("\"es\":{\"title\":\"...\",\"description\":\"...\",\"expectedImpact\":\"...\",\"urgencyWindow\":\"...\",\"rationale\":[\"...\"],\"recommendedActions\":[\"...\"]}");
        prompt.append("}");
        prompt.append("}\n");
        prompt.append("For non-epidemiology recommendations, do not rename the English recommendation title; translations.en.title must exactly match the current draft title.\n");
        prompt.append("For EPIDEMIOLOGY_* recommendations, translations.en.title may refine the current draft title, but it must preserve the selected disease and scope from the Live grounding context.\n");
        prompt.append("For EPIDEMIOLOGY_MUNICIPAL, the scope is a radius of surrounding municipalities around the hospital; do not title it as a single municipality even if hospitalGeoScope has a municipalityName.\n");
        prompt.append("Use titles like \"[Disease] readiness in surrounding municipalities\" / \"Preparacion ante [Disease] en municipios circundantes\" for EPIDEMIOLOGY_MUNICIPAL.\n");
        prompt.append("Use municipality names from evidence only as supporting detail in description, rationale, or actions when useful.\n");
        prompt.append("translations.es.title should be a faithful Spanish translation for Mexico.\n");
        prompt.append("Descriptions should stay concise and operational. Rationale and recommendedActions should each have 2 to 4 items in each language.\n");
        prompt.append("Treat the current title and description as a draft hint, not as the main source of truth.\n\n");
        prompt.append("Recommendation intent:\n");
        prompt.append("- Type: ").append(valueOrFallback(recommendation.getType(), "Unknown")).append("\n");
        prompt.append("- Severity: ").append(valueOrFallback(recommendation.getSeverity(), "Unknown")).append("\n");
        prompt.append("- Current draft title: ").append(valueOrFallback(recommendation.getTitle(), "Unknown")).append("\n");
        prompt.append("- Current draft description: ").append(valueOrFallback(recommendation.getDescription(), "None")).append("\n");
        prompt.append("- Current expected impact: ").append(valueOrFallback(recommendation.getExpectedImpact(), "None")).append("\n");
        prompt.append("- Current urgency window: ").append(valueOrFallback(recommendation.getUrgencyWindow(), "None")).append("\n");
        prompt.append("- Affected departments JSON: ").append(valueOrFallback(recommendation.getAffectedDepartmentsJson(), "[]")).append("\n");
        prompt.append("- Affected resources JSON: ").append(valueOrFallback(recommendation.getAffectedResourcesJson(), "[]")).append("\n\n");
        prompt.append("Live grounding context:\n");
        prompt.append(valueOrFallback(recommendation.getInputContextJson(), "{}")).append("\n");
        return prompt.toString();
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
