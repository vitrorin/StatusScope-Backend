package com.itesm.application.usecase;

import com.itesm.application.dto.DiagnosisDiseaseOptionDto;
import com.itesm.infrastructure.persistence.entity.DiseaseEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class ListDiagnosisDiseasesUseCase {

    private static final int DEFAULT_LIMIT = 12;
    private static final int MAX_LIMIT = 50;

    @Inject
    EntityManager entityManager;

    public List<DiagnosisDiseaseOptionDto> list(String query, Integer limit) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        int normalizedLimit = normalizeLimit(limit);

        return entityManager.createQuery("""
                select d
                from DiseaseEntity d
                where :query = ''
                   or lower(d.name) like :pattern
                   or lower(d.code) like :pattern
                order by d.name asc
                """, DiseaseEntity.class)
                .setParameter("query", normalizedQuery)
                .setParameter("pattern", "%" + normalizedQuery + "%")
                .setMaxResults(normalizedLimit)
                .getResultStream()
                .map(this::toDto)
                .toList();
    }

    private DiagnosisDiseaseOptionDto toDto(DiseaseEntity disease) {
        DiagnosisDiseaseOptionDto dto = new DiagnosisDiseaseOptionDto();
        dto.setId(disease.getId());
        dto.setCode(disease.getCode());
        dto.setName(disease.getName());
        return dto;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }
}
