package com.itesm.infrastructure.mapper;

import com.itesm.domain.models.User;
import com.itesm.infrastructure.persistence.entity.RoleEntity;
import com.itesm.infrastructure.persistence.entity.UserEntity;

import java.util.HashSet;
import java.util.stream.Collectors;

public final class UserMapper {
    private UserMapper() {
    }

    public static User toDomain(UserEntity entity) {
        User user = new User();
        user.setId(entity.getId());
        user.setFullName(entity.getFullName());
        user.setEmail(entity.getEmail());
        user.setActive(entity.isActive());
        user.setExternalAuthId(entity.getExternalAuthId());
        user.setRoles(entity.getRoles().stream().map(RoleMapper::toDomain).collect(Collectors.toSet()));
        return user;
    }

    public static UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.getId());
        entity.setFullName(user.getFullName());
        entity.setEmail(user.getEmail());
        entity.setActive(user.isActive());
        entity.setExternalAuthId(user.getExternalAuthId());
        entity.setRoles(new HashSet<>());
        for (com.itesm.domain.models.Role role : user.getRoles()) {
            RoleEntity roleEntity = new RoleEntity();
            roleEntity.setId(role.getId());
            entity.getRoles().add(roleEntity);
        }
        return entity;
    }
}
