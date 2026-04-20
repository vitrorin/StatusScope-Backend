package com.itesm.infrastructure.mapper;

import com.itesm.domain.models.Privilege;
import com.itesm.infrastructure.persistence.entity.PrivilegeEntity;

public final class PrivilegeMapper {
    private PrivilegeMapper() {
    }

    public static Privilege toDomain(PrivilegeEntity entity) {
        Privilege privilege = new Privilege();
        privilege.setId(entity.getId());
        privilege.setCode(entity.getCode());
        privilege.setDescription(entity.getDescription());
        return privilege;
    }
}
