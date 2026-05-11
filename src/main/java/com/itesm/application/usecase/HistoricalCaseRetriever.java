package com.itesm.application.usecase;

import com.itesm.infrastructure.persistence.entity.PatientEvaluationEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Retrieves prior confirmed evaluations from the same hospital that look similar
 * to the current case, so they can be injected into the assistant prompt as
 * grounding context. v1 is keyword-overlap on symptoms_text; replace with a
 * vector store later if quality requires it.
 */
@ApplicationScoped
public class HistoricalCaseRetriever {

    private static final int CANDIDATE_WINDOW = 60;
    private static final int DEFAULT_TOP_N = 5;
    private static final int LOOKBACK_MONTHS = 12;

    private static final Pattern TOKENIZER = Pattern.compile("[^a-z0-9]+");
    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "with", "for", "from", "have", "has", "had", "this", "that",
            "patient", "patients", "reports", "reported", "history", "no", "not",
            "of", "in", "on", "at", "to", "a", "an", "is", "are", "was", "were",
            "since", "ago", "days", "day", "weeks", "week", "months", "month",
            "year", "years", "old"
    );

    @Inject
    EntityManager entityManager;

    public List<HistoricalCase> retrieveSimilar(UUID hospitalId,
                                                UUID currentEvaluationId,
                                                String currentSymptoms) {
        if (hospitalId == null) {
            return List.of();
        }

        LocalDateTime since = LocalDateTime.now().minusMonths(LOOKBACK_MONTHS);

        List<PatientEvaluationEntity> candidates = entityManager.createQuery("""
                select e
                from PatientEvaluationEntity e
                join fetch e.patient p
                left join fetch e.finalDisease d
                where e.id <> :currentId
                  and p.hospital.id = :hospitalId
                  and e.status = 'CONFIRMED'
                  and e.finalizedAt is not null
                  and e.finalizedAt >= :since
                order by e.finalizedAt desc
                """, PatientEvaluationEntity.class)
                .setParameter("currentId", currentEvaluationId == null ? UUID.randomUUID() : currentEvaluationId)
                .setParameter("hospitalId", hospitalId)
                .setParameter("since", since)
                .setMaxResults(CANDIDATE_WINDOW)
                .getResultList();

        if (candidates.isEmpty()) {
            return List.of();
        }

        Set<String> currentTokens = tokenize(currentSymptoms);

        List<HistoricalCase> ranked = new ArrayList<>();
        for (PatientEvaluationEntity e : candidates) {
            double score = scoreOverlap(currentTokens, tokenize(e.getSymptomsText()));
            String diseaseName = e.getFinalDisease() != null ? e.getFinalDisease().getName() : null;
            String finalLabel = e.getFinalDiagnosisLabel();
            String displayDiagnosis = diseaseName != null
                    ? diseaseName
                    : (finalLabel != null ? finalLabel : "Unspecified");

            ranked.add(new HistoricalCase(
                    e.getId(),
                    e.getFinalizedAt(),
                    ageYears(e.getPatient().getBirthDate()),
                    e.getPatient().getSex(),
                    e.getSymptomsText(),
                    displayDiagnosis,
                    score
            ));
        }

        ranked.sort(Comparator
                .comparingDouble(HistoricalCase::similarityScore).reversed()
                .thenComparing(Comparator.comparing(HistoricalCase::finalizedAt).reversed()));

        return ranked.subList(0, Math.min(DEFAULT_TOP_N, ranked.size()));
    }

    private static Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        Set<String> tokens = new HashSet<>();
        for (String token : TOKENIZER.split(text.toLowerCase(Locale.ROOT))) {
            if (token.length() < 3) continue;
            if (STOPWORDS.contains(token)) continue;
            tokens.add(token);
        }
        return tokens;
    }

    private static double scoreOverlap(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0d;
        }
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        if (intersection.isEmpty()) {
            return 0d;
        }
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / (double) union.size();
    }

    private static Integer ageYears(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public record HistoricalCase(
            UUID evaluationId,
            LocalDateTime finalizedAt,
            Integer ageYears,
            String sex,
            String symptoms,
            String confirmedDiagnosis,
            double similarityScore
    ) {}
}
