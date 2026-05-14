package com.itesm.application.usecase;

import com.itesm.application.dto.PatientContextDto;
import com.itesm.domain.models.Outbreak;
import com.itesm.domain.models.State;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.format.DateTimeFormatter;
import java.util.List;

@ApplicationScoped
public class AssistantPromptBuilder {

    private static final DateTimeFormatter HISTORICAL_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    public String build(State state, List<Outbreak> outbreaks, PatientContextDto patientContext) {
        return build(state, outbreaks, patientContext, 0, List.of());
    }

    public String build(State state, List<Outbreak> outbreaks, PatientContextDto patientContext, double radiusKm) {
        return build(state, outbreaks, patientContext, radiusKm, List.of());
    }

    public String build(State state,
                        List<Outbreak> outbreaks,
                        PatientContextDto patientContext,
                        List<HistoricalCaseRetriever.HistoricalCase> historicalCases) {
        return build(state, outbreaks, patientContext, 0, historicalCases);
    }

    public String build(State state,
                        List<Outbreak> outbreaks,
                        PatientContextDto patientContext,
                        double radiusKm,
                        List<HistoricalCaseRetriever.HistoricalCase> historicalCases) {
        StringBuilder sb = new StringBuilder();

        String stateName = state != null ? state.getName() : "an unknown state";
        sb.append("You are a clinical decision-support assistant for a doctor in ").append(stateName).append(".\n");
        sb.append("Reply concisely. Do not invent diagnoses you cannot justify from the symptoms.\n");

        if (outbreaks != null && !outbreaks.isEmpty()) {
            sb.append("\nActive outbreaks in the hospital geographic radius");
            if (radiusKm > 0) {
                sb.append(" (~").append(Math.round(radiusKm)).append(" km)");
            }
            sb.append(" (use these to bias differential diagnosis only when symptoms overlap):\n");
            for (Outbreak o : outbreaks) {
                if (o.getDisease() == null) continue;
                sb.append("  - ").append(o.getDisease().getName())
                  .append(": ").append(o.getCaseCount())
                  .append(" active cases since ").append(o.getStartedAt()).append(".\n");
                if (o.getScope() != null && !o.getScope().isBlank()) {
                    sb.append("    Scope: ").append(o.getScope()).append(".\n");
                }
                if (o.getConfirmationStatus() != null && !o.getConfirmationStatus().isBlank()) {
                    sb.append("    Confirmation status: ").append(o.getConfirmationStatus()).append(".\n");
                }
                if (o.getMunicipality() != null) {
                    sb.append("    Location: ").append(o.getMunicipality().getName());
                    if (o.getState() != null && o.getState().getName() != null) {
                        sb.append(", ").append(o.getState().getName());
                    }
                    sb.append(".\n");
                } else if (o.getState() != null && o.getState().getName() != null) {
                    sb.append("    Location: ").append(o.getState().getName()).append(".\n");
                }
                if (o.getDisease().getSymptoms() != null && !o.getDisease().getSymptoms().isBlank()) {
                    sb.append("    Hallmark symptoms: ").append(o.getDisease().getSymptoms()).append(".\n");
                }
            }
        }

        if (historicalCases != null && !historicalCases.isEmpty()) {
            sb.append("\nSimilar confirmed cases at this hospital in the last 12 months ")
              .append("(grounding context - do not assume the same diagnosis applies):\n");
            for (HistoricalCaseRetriever.HistoricalCase hc : historicalCases) {
                sb.append("  - ");
                if (hc.ageYears() != null) sb.append(hc.ageYears()).append("y ");
                if (hc.sex() != null && !hc.sex().isBlank()) sb.append(hc.sex(), 0, 1).append(" ");
                sb.append("symptoms: ").append(truncate(hc.symptoms(), 160));
                sb.append(" -> Confirmed: ").append(hc.confirmedDiagnosis());
                if (hc.finalizedAt() != null) {
                    sb.append(" (").append(hc.finalizedAt().toLocalDate().format(HISTORICAL_DATE)).append(")");
                }
                sb.append("\n");
            }
        }

        if (patientContext != null) {
            sb.append("\nPatient under evaluation:\n");
            if (patientContext.getAgeYears() != null) {
                sb.append("  - Age: ").append(patientContext.getAgeYears()).append("\n");
            }
            if (patientContext.getSex() != null && !patientContext.getSex().isBlank()) {
                sb.append("  - Sex: ").append(patientContext.getSex()).append("\n");
            }
            if (patientContext.getSymptoms() != null && !patientContext.getSymptoms().isBlank()) {
                sb.append("  - Reported symptoms: ").append(patientContext.getSymptoms()).append("\n");
            }
        }

        if (outbreaks != null && !outbreaks.isEmpty()) {
            sb.append("\nWhen the patient's reported symptoms overlap with an active outbreak, ")
              .append("explicitly call this out and recommend confirming tests.\n");
        }

        sb.append("\nAfter your free-text reply, append a single block on a new line with the structured ")
          .append("differential, exactly in this format and nothing else after it:\n")
          .append("<DIAGNOSIS_JSON>{\"differentialDiagnoses\":[")
          .append("{\"displayName\":\"...\",\"confidence\":0.0,\"rationale\":\"...\",")
          .append("\"localityRiskLevel\":\"HIGH|MEDIUM|LOW|NONE\",\"isPrimary\":true}")
          .append("]}</DIAGNOSIS_JSON>\n")
          .append("Use up to 5 entries ranked by confidence (0..1). Set isPrimary=true on the top one only. ")
          .append("Set localityRiskLevel=HIGH only when the diagnosis matches an active outbreak above. ")
          .append("Omit the block entirely if you cannot suggest any differential.\n");

        return sb.toString().trim();
    }

    private static String truncate(String value, int max) {
        if (value == null) return "";
        String collapsed = value.replaceAll("\\s+", " ").trim();
        if (collapsed.length() <= max) return collapsed;
        return collapsed.substring(0, max - 3) + "...";
    }
}
