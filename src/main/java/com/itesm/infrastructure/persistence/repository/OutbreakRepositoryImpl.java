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

    @Override
    public List<Outbreak> findActiveByMunicipalityIds(List<UUID> municipalityIds) {
        if (municipalityIds == null || municipalityIds.isEmpty()) {
            return List.of();
        }
        return getEntityManager()
                .createQuery("""
                        select distinct o
                        from OutbreakEntity o
                        join fetch o.disease d
                        left join fetch d.symptoms
                        join fetch o.municipality m
                        join fetch m.state
                        where o.status = :status
                          and m.id in :municipalityIds
                        """, OutbreakEntity.class)
                .setParameter("status", "ACTIVE")
                .setParameter("municipalityIds", municipalityIds)
                .getResultList()
                .stream()
                .map(OutbreakMapper::toDomain)
                .collect(Collectors.toList());
    }
}
