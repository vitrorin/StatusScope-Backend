package com.itesm.infrastructure.persistence.repository;

import com.itesm.domain.models.User;
import com.itesm.domain.repository.UserRepository;
import com.itesm.infrastructure.mapper.UserMapper;
import com.itesm.infrastructure.persistence.entity.UserEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserRepositoryImpl implements UserRepository, PanacheRepositoryBase<UserEntity, UUID> {

    @Override
    public Optional<User> findByEmail(String email) {
        return findWithRolesAndPrivileges("email", email);
    }

    @Override
    public Optional<User> findByExternalAuthId(String externalAuthId) {
        return findWithRolesAndPrivileges("externalAuthId", externalAuthId);
    }

    @Override
    public Optional<User> findUserById(UUID id) {
        return findWithRolesAndPrivileges("id", id);
    }

    @Override
    @Transactional
    public User create(User user) {
        UserEntity entity = UserMapper.toEntity(user);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        persist(entity);
        getEntityManager().flush();
        getEntityManager().clear();
        return findUserById(entity.getId()).orElse(UserMapper.toDomain(entity));
    }

    @Override
    @Transactional
    public User update(User user) {
        UserEntity managed = find("select distinct u from UserEntity u left join fetch u.roles r left join fetch r.privileges where u.id = ?1", user.getId())
                .firstResult();
        managed.setFullName(user.getFullName());
        managed.setEmail(user.getEmail());
        managed.setActive(user.isActive());
        managed.setExternalAuthId(user.getExternalAuthId());
        managed.setUpdatedAt(LocalDateTime.now());

        managed.getRoles().clear();
        managed.getRoles().addAll(UserMapper.toEntity(user).getRoles());

        persist(managed);
        getEntityManager().flush();
        getEntityManager().clear();
        return findUserById(managed.getId()).orElse(UserMapper.toDomain(managed));
    }

    private Optional<User> findWithRolesAndPrivileges(String field, Object value) {
        return find("select distinct u from UserEntity u left join fetch u.roles r left join fetch r.privileges where u." + field + " = ?1", value)
                .firstResultOptional()
                .map(UserMapper::toDomain);
    }
}
