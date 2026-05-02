package com.itesm.application.usecase;

import com.itesm.application.dto.PatientContextDto;
import com.itesm.domain.models.Outbreak;
import com.itesm.domain.models.State;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class AssistantPromptBuilder {

    public String build(State state, List<Outbreak> outbreaks, PatientContextDto patientContext) {
        return build(state, outbreaks, patientContext, 0);
    }

    public String build(State state, List<Outbreak> outbreaks, PatientContextDto patientContext, double radiusKm) {
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
                if (o.getMunicipality() != null) {
                    sb.append("    Location: ").append(o.getMunicipality().getName());
                    if (o.getState() != null && o.getState().getName() != null) {
                        sb.append(", ").append(o.getState().getName());
                    }
                    sb.append(".\n");
                }
                if (o.getDisease().getSymptoms() != null && !o.getDisease().getSymptoms().isBlank()) {
                    sb.append("    Hallmark symptoms: ").append(o.getDisease().getSymptoms()).append(".\n");
                }
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

        return sb.toString().trim();
    }
}
