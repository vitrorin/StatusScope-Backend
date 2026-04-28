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
    public List<Outbreak> findActiveByRegionId(UUID regionId) {
        return find("status = ?1 and region.id = ?2", "ACTIVE", regionId)
                .stream()
                .map(OutbreakMapper::toDomain)
                .collect(Collectors.toList());
    }
}
