package com.itesm.application.usecase;

import com.itesm.application.dto.PatientContextDto;
import com.itesm.domain.models.Outbreak;
import com.itesm.domain.models.Region;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class AssistantPromptBuilder {

    public String build(Region region, List<Outbreak> outbreaks, PatientContextDto patientContext) {
        StringBuilder sb = new StringBuilder();

        String regionName = region != null ? region.getName() : "an unknown region";
        sb.append("You are a clinical decision-support assistant for a doctor in ").append(regionName).append(".\n");
        sb.append("Reply concisely. Do not invent diagnoses you cannot justify from the symptoms.\n");

        if (outbreaks != null && !outbreaks.isEmpty()) {
            sb.append("\nActive outbreaks in this region (use these to bias differential diagnosis):\n");
            for (Outbreak o : outbreaks) {
                if (o.getDisease() == null) continue;
                sb.append("  - ").append(o.getDisease().getName())
                  .append(": ").append(o.getCaseCount())
                  .append(" active cases since ").append(o.getStartedAt()).append(".\n");
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
