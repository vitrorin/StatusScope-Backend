package com.itesm.infrastructure.persistence.repository;

import com.itesm.domain.models.Outbreak;
import com.itesm.domain.repository.OutbreakRepository;
import com.itesm.infrastructure.mapper.OutbreakMapper;
import com.itesm.infrastructure.persistence.entity.OutbreakEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class OutbreakRepositoryImpl implements OutbreakRepository, PanacheRepositoryBase<OutbreakEntity, UUID> {

    private static final UUID NO_MUNICIPALITY_MATCH = new UUID(0, 0);

    @Override
    public List<Outbreak> findAllActiveMunicipal() {
        return getEntityManager()
                .createQuery("""
                        select distinct o
                        from OutbreakEntity o
                        join fetch o.disease d
                        left join fetch d.symptoms
                        join fetch o.municipality m
                        left join fetch m.state
                        where o.status = :status
                          and o.scope = 'MUNICIPALITY'
                        """, OutbreakEntity.class)
                .setParameter("status", "ACTIVE")
                .getResultList()
                .stream()
                .map(OutbreakMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Outbreak> findActiveMunicipalByStateId(UUID stateId) {
        if (stateId == null) return List.of();
        return getEntityManager()
                .createQuery("""
                        select distinct o
                        from OutbreakEntity o
                        join fetch o.disease d
                        left join fetch d.symptoms
                        join fetch o.municipality m
                        left join fetch m.state s
                        where o.status = :status
                          and o.scope = 'MUNICIPALITY'
                          and s.id = :stateId
                        """, OutbreakEntity.class)
                .setParameter("status", "ACTIVE")
                .setParameter("stateId", stateId)
                .getResultList()
                .stream()
                .map(OutbreakMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Outbreak> findActiveStateByStateId(UUID stateId) {
        if (stateId == null) return List.of();
        return getEntityManager()
                .createQuery("""
                        select distinct o
                        from OutbreakEntity o
                        join fetch o.disease d
                        left join fetch d.symptoms
                        join fetch o.state s
                        where o.status = :status
                          and o.scope = 'STATE'
                          and s.id = :stateId
                        """, OutbreakEntity.class)
                .setParameter("status", "ACTIVE")
                .setParameter("stateId", stateId)
                .getResultList()
                .stream()
                .map(OutbreakMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Outbreak> findActiveByMunicipalityIds(List<UUID> municipalityIds) {
        return findActiveByMunicipalityIdsOrStateId(municipalityIds, null).stream()
                .filter(outbreak -> "MUNICIPALITY".equals(outbreak.getScope()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Outbreak> findActiveByMunicipalityIdsOrStateId(List<UUID> municipalityIds, UUID stateId) {
        if (municipalityIds == null || municipalityIds.isEmpty()) {
            municipalityIds = List.of();
        }
        boolean hasMunicipalities = !municipalityIds.isEmpty();
        boolean hasState = stateId != null;
        if (!hasMunicipalities && !hasState) return List.of();
        List<UUID> queryMunicipalityIds = hasMunicipalities ? municipalityIds : List.of(NO_MUNICIPALITY_MATCH);

        return getEntityManager()
                .createQuery("""
                        select distinct o
                        from OutbreakEntity o
                        join fetch o.disease d
                        left join fetch d.symptoms
                        left join fetch o.municipality m
                        left join fetch m.state
                        left join fetch o.state s
                        where o.status = :status
                          and (
                              (:hasMunicipalities = true and o.scope = 'MUNICIPALITY' and m.id in :municipalityIds)
                              or (:hasState = true and o.scope = 'STATE' and s.id = :stateId)
                          )
                        """, OutbreakEntity.class)
                .setParameter("status", "ACTIVE")
                .setParameter("municipalityIds", queryMunicipalityIds)
                .setParameter("stateId", stateId)
                .setParameter("hasMunicipalities", hasMunicipalities)
                .setParameter("hasState", hasState)
                .getResultList()
                .stream()
                .map(OutbreakMapper::toDomain)
                .collect(Collectors.toList());
    }
}
