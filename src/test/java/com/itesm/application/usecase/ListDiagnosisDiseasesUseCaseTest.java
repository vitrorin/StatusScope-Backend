package com.itesm.application.usecase;

import com.itesm.infrastructure.persistence.entity.DiseaseEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.stream.Stream;

class ListDiagnosisDiseasesUseCaseTest {

    private ListDiagnosisDiseasesUseCase useCase;
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        useCase = new ListDiagnosisDiseasesUseCase();
        entityManager = Mockito.mock(EntityManager.class);
        useCase.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    private TypedQuery<DiseaseEntity> mockTypedQuery() {
        TypedQuery<DiseaseEntity> query = Mockito.mock(TypedQuery.class);
        Mockito.when(entityManager.createQuery(Mockito.anyString(), Mockito.eq(DiseaseEntity.class)))
                .thenReturn(query);
        Mockito.when(query.setParameter(Mockito.anyString(), Mockito.any())).thenReturn(query);
        Mockito.when(query.setMaxResults(Mockito.anyInt())).thenReturn(query);
        Mockito.when(query.getResultStream()).thenReturn(Stream.empty());
        return query;
    }

    // ── Limit normalization ───────────────────────────────────────────────────

    @Test
    void shouldUseDefaultLimitWhenNull() {
        TypedQuery<DiseaseEntity> query = mockTypedQuery();
        useCase.list(null, null);
        Mockito.verify(query).setMaxResults(12); // DEFAULT_LIMIT = 12
    }

    @Test
    void shouldCapLimitAtMax() {
        TypedQuery<DiseaseEntity> query = mockTypedQuery();
        useCase.list("dengue", 100); // MAX = 50
        Mockito.verify(query).setMaxResults(50);
    }

    @Test
    void shouldEnforceMinLimitOfOne() {
        TypedQuery<DiseaseEntity> query = mockTypedQuery();
        useCase.list("flu", 0);
        Mockito.verify(query).setMaxResults(1);
    }

    @Test
    void shouldRespectValidLimitWithinBounds() {
        TypedQuery<DiseaseEntity> query = mockTypedQuery();
        useCase.list("flu", 8);
        Mockito.verify(query).setMaxResults(8);
    }

    @Test
    void shouldRespectMaxBoundary() {
        TypedQuery<DiseaseEntity> query = mockTypedQuery();
        useCase.list("flu", 50);
        Mockito.verify(query).setMaxResults(50);
    }

    // ── Query normalization ───────────────────────────────────────────────────

    @Test
    void shouldNormalizeQueryToLowercase() {
        TypedQuery<DiseaseEntity> query = mockTypedQuery();
        useCase.list("DENGUE", 10);
        Mockito.verify(query).setParameter("query", "dengue");
    }

    @Test
    void shouldTrimQueryBeforeNormalization() {
        TypedQuery<DiseaseEntity> query = mockTypedQuery();
        useCase.list("  dengue  ", 10);
        Mockito.verify(query).setParameter("query", "dengue");
    }

    @Test
    void shouldHandleNullQueryAsEmptyString() {
        TypedQuery<DiseaseEntity> query = mockTypedQuery();
        useCase.list(null, 10);
        Mockito.verify(query).setParameter("query", "");
    }

    @Test
    void shouldPassEmptyStringQueryThrough() {
        TypedQuery<DiseaseEntity> query = mockTypedQuery();
        useCase.list("", 10);
        Mockito.verify(query).setParameter("query", "");
    }

    // ── Pattern construction ──────────────────────────────────────────────────

    @Test
    void shouldBuildLikePatternWithWildcards() {
        TypedQuery<DiseaseEntity> query = mockTypedQuery();
        useCase.list("flu", 10);
        Mockito.verify(query).setParameter("pattern", "%flu%");
    }

    @Test
    void shouldBuildPatternWithEmptyQueryAsWildcardOnly() {
        TypedQuery<DiseaseEntity> query = mockTypedQuery();
        useCase.list("", 10);
        Mockito.verify(query).setParameter("pattern", "%%");
    }

    // ── Return value ──────────────────────────────────────────────────────────

    @Test
    void shouldReturnEmptyListWhenNoResults() {
        mockTypedQuery();
        var result = useCase.list("xyz-nonexistent", 10);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }
}
