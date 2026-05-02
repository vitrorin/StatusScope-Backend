package com.itesm.infrastructure.mapper;

import com.itesm.domain.models.Municipality;
import com.itesm.infrastructure.persistence.entity.MunicipalityEntity;

public final class MunicipalityMapper {
    private MunicipalityMapper() {}

    public static Municipality toDomain(MunicipalityEntity entity) {
        Municipality municipality = new Municipality();
        municipality.setId(entity.getId());
        municipality.setCode(entity.getCode());
        municipality.setName(entity.getName());
        municipality.setLatitude(entity.getLatitude());
        municipality.setLongitude(entity.getLongitude());
        if (entity.getCity() != null) {
            municipality.setCityId(entity.getCity().getId());
            municipality.setCityName(entity.getCity().getName());
            if (entity.getCity().getState() != null) {
                municipality.setStateId(entity.getCity().getState().getId());
                municipality.setStateName(entity.getCity().getState().getName());
            }
        }
        return municipality;
    }
}
