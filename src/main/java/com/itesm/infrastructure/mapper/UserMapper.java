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
        user.setStatus(entity.getStatus());
        user.setLastLoginAt(entity.getLastLoginAt());
        user.setLicenseNumber(entity.getLicenseNumber());
        user.setSpecialtyId(entity.getSpecialty() == null ? null : entity.getSpecialty().getId());
        user.setHospitalId(entity.getHospital() == null ? null : entity.getHospital().getId());
        user.setRoles(entity.getRoles() == null ? new HashSet<>() :
                entity.getRoles().stream().map(RoleMapper::toDomain).collect(Collectors.toSet()));
        return user;
    }

    public static UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.getId());
        entity.setFullName(user.getFullName());
        entity.setEmail(user.getEmail());
        entity.setActive(user.isActive());
        entity.setExternalAuthId(user.getExternalAuthId());
        entity.setStatus(user.getStatus());
        entity.setLastLoginAt(user.getLastLoginAt());
        entity.setLicenseNumber(user.getLicenseNumber());
        entity.setRoles(new HashSet<>());
        if (user.getRoles() != null) {
            for (com.itesm.domain.models.Role role : user.getRoles()) {
                RoleEntity re = new RoleEntity();
                re.setId(role.getId());
                entity.getRoles().add(re);
            }
        }
        return entity;
    }
}
