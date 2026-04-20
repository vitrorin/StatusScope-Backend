package com.itesm.infrastructure.mapper;

import com.itesm.domain.models.Role;
import com.itesm.infrastructure.persistence.entity.RoleEntity;

import java.util.stream.Collectors;

public final class RoleMapper {
    private RoleMapper() {
    }

    public static Role toDomain(RoleEntity entity) {
        Role role = new Role();
        role.setId(entity.getId());
        role.setCode(entity.getCode());
        role.setName(entity.getName());
        role.setDescription(entity.getDescription());
        role.setPrivileges(entity.getPrivileges().stream().map(PrivilegeMapper::toDomain).collect(Collectors.toSet()));
        return role;
    }
}
