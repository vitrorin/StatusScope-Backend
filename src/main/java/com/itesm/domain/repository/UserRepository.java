package com.itesm.domain.repository;

import com.itesm.domain.models.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findByEmail(String email);

    Optional<User> findByExternalAuthId(String externalAuthId);

    Optional<User> findUserById(UUID id);

    List<User> listAllDomain();

    List<User> findByHospitalId(UUID hospitalId);

    User create(User user);

    User update(User user);

    void updateLastLoginAt(UUID userId, LocalDateTime lastLoginAt);
}
