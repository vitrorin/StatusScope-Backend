package com.itesm.domain.repository;

import com.itesm.domain.models.Role;

import java.util.List;
import java.util.Optional;

public interface RoleRepository {
    Optional<Role> findByCode(String code);

    List<Role> listAllRoles();
}
