package com.itesm.infrastructure.persistence.repository;

import com.itesm.domain.models.Role;
import com.itesm.domain.repository.RoleRepository;
import com.itesm.infrastructure.mapper.RoleMapper;
import com.itesm.infrastructure.persistence.entity.RoleEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class RoleRepositoryImpl implements RoleRepository, PanacheRepositoryBase<RoleEntity, UUID> {

    @Override
    public Optional<Role> findByCode(String code) {
        return find("select distinct r from RoleEntity r left join fetch r.privileges where r.code = ?1", code)
                .firstResultOptional()
                .map(RoleMapper::toDomain);
    }

    @Override
    public List<Role> listAllRoles() {
        return find("select distinct r from RoleEntity r left join fetch r.privileges")
                .list()
                .stream()
                .map(RoleMapper::toDomain)
                .collect(Collectors.toList());
    }
}
