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
        return find("status = ?1 and municipality.id in ?2", "ACTIVE", municipalityIds)
                .stream()
                .map(OutbreakMapper::toDomain)
                .collect(Collectors.toList());
    }
}
