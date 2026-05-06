package com.itesm.infrastructure.persistence.repository;

import com.itesm.domain.models.Municipality;
import com.itesm.domain.repository.MunicipalityRepository;
import com.itesm.infrastructure.mapper.MunicipalityMapper;
import com.itesm.infrastructure.persistence.entity.MunicipalityEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class MunicipalityRepositoryImpl implements MunicipalityRepository, PanacheRepositoryBase<MunicipalityEntity, UUID> {

    @Override
    public List<Municipality> listAllDomain() {
        return findAll().stream().map(MunicipalityMapper::toDomain).collect(Collectors.toList());
    }
}
