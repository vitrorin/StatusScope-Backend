package com.itesm.domain.repository;

import com.itesm.domain.models.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findByEmail(String email);

    Optional<User> findByExternalAuthId(String externalAuthId);

    Optional<User> findUserById(UUID id);

    User create(User user);

    User update(User user);
}
