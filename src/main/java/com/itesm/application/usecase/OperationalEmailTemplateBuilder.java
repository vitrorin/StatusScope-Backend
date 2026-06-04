package com.itesm.application.usecase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itesm.application.port.out.OperationalEmailMessage;
import com.itesm.domain.models.OperationalRecommendation;
import com.itesm.domain.models.OperationalTask;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class OperationalEmailTemplateBuilder {

    @Inject ObjectMapper objectMapper;

    public OperationalEmailMessage taskAssignment(String to, OperationalRecommendation rec, OperationalTask task, String recipientName) {
        boolean spanish = isSpanish(task.getLanguage());
        Content content = contentFor(rec, spanish);
        String subject = spanish
                ? "Nueva tarea operativa: " + content.title()
                : "New operational task: " + content.title();
        String body = String.join("\n",
                greeting(spanish, recipientName),
                "",
                spanish ? "Se te asigno una tarea operativa desde StatuScope." : "You have been assigned an operational task from StatuScope.",
                "",
                label(spanish, "Recomendacion", "Recommendation") + content.title(),
                label(spanish, "Descripcion", "Description") + value(content.description(), "N/A"),
                label(spanish, "Prioridad", "Priority") + value(task.getPriority(), rec.getSeverity()),
                label(spanish, "Ventana", "Window") + value(content.urgencyWindow(), rec.getUrgencyWindow()),
                label(spanish, "Impacto", "Impact") + value(content.expectedImpact(), rec.getExpectedImpact()),
                label(spanish, "Fecha limite", "Deadline") + (task.getDeadlineAt() != null ? task.getDeadlineAt().toString() : "N/A"),
                label(spanish, "Notas del administrador", "Admin notes") + value(task.getNotes(), "N/A"),
                "",
                actionsTitle(spanish),
                actionsList(content.recommendedActions()),
                "",
                spanish
                        ? "Por favor revisa esta indicacion y coordina la respuesta correspondiente."
                        : "Please review this guidance and coordinate the appropriate response."
        );
        return new OperationalEmailMessage(to, subject, body);
    }

    public OperationalEmailMessage staffNotification(String to, OperationalRecommendation rec, String message, String recipientName, String language) {
        boolean spanish = isSpanish(language);
        Content content = contentFor(rec, spanish);
        String subject = spanish
                ? "Aviso operativo: " + content.title()
                : "Operational notice: " + content.title();
        String body = String.join("\n",
                greeting(spanish, recipientName),
                "",
                spanish ? "Recibiste un aviso operativo desde StatuScope." : "You received an operational notice from StatuScope.",
                "",
                label(spanish, "Recomendacion", "Recommendation") + content.title(),
                label(spanish, "Descripcion", "Description") + value(content.description(), "N/A"),
                label(spanish, "Prioridad", "Priority") + value(rec.getSeverity(), "N/A"),
                label(spanish, "Ventana", "Window") + value(content.urgencyWindow(), rec.getUrgencyWindow()),
                label(spanish, "Impacto", "Impact") + value(content.expectedImpact(), rec.getExpectedImpact()),
                label(spanish, "Mensaje del administrador", "Admin message") + value(message, "N/A"),
                "",
                actionsTitle(spanish),
                actionsList(content.recommendedActions())
        );
        return new OperationalEmailMessage(to, subject, body);
    }

    private Content contentFor(OperationalRecommendation rec, boolean spanish) {
        String locale = spanish ? "es" : "en";
        try {
            if (rec.getContentTranslationsJson() != null && !rec.getContentTranslationsJson().isBlank()) {
                JsonNode root = objectMapper.readTree(rec.getContentTranslationsJson());
                JsonNode node = root.get(locale);
                if (node != null && node.isObject()) {
                    return new Content(
                            text(node, "title", rec.getTitle()),
                            text(node, "description", rec.getDescription()),
                            text(node, "expectedImpact", rec.getExpectedImpact()),
                            text(node, "urgencyWindow", rec.getUrgencyWindow()),
                            strings(node, "recommendedActions", parseStrings(rec.getRecommendedActionsJson())));
                }
            }
        } catch (Exception ignored) {
        }
        return new Content(
                rec.getTitle(),
                rec.getDescription(),
                rec.getExpectedImpact(),
                rec.getUrgencyWindow(),
                parseStrings(rec.getRecommendedActionsJson()));
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() && !value.asText().isBlank() ? value.asText() : fallback;
    }

    private List<String> strings(JsonNode node, String field, List<String> fallback) {
        JsonNode value = node.get(field);
        if (value == null || !value.isArray()) return fallback;
        List<String> output = new ArrayList<>();
        value.forEach(item -> {
            if (item.isTextual() && !item.asText().isBlank()) output.add(item.asText());
        });
        return output.isEmpty() ? fallback : output;
    }

    private List<String> parseStrings(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(rawJson, new TypeReference<List<String>>() {});
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private boolean isSpanish(String language) {
        return language == null || !"en".equalsIgnoreCase(language.trim());
    }

    private String greeting(boolean spanish, String recipientName) {
        String name = value(recipientName, spanish ? "equipo" : "team");
        return spanish ? "Hola " + name + "," : "Hello " + name + ",";
    }

    private String label(boolean spanish, String es, String en) {
        return (spanish ? es : en) + ": ";
    }

    private String actionsTitle(boolean spanish) {
        return spanish ? "Acciones a implementar:" : "Actions to implement:";
    }

    private String actionsList(List<String> actions) {
        if (actions == null || actions.isEmpty()) return "- N/A";
        return actions.stream().map(action -> "- " + action).reduce((a, b) -> a + "\n" + b).orElse("- N/A");
    }

    private String value(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private record Content(
            String title,
            String description,
            String expectedImpact,
            String urgencyWindow,
            List<String> recommendedActions) {}
}
