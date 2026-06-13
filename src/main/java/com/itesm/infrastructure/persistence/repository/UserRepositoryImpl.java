package com.itesm.infrastructure.persistence.repository;

import com.itesm.domain.models.User;
import com.itesm.domain.repository.UserRepository;
import com.itesm.infrastructure.mapper.UserMapper;
import com.itesm.infrastructure.persistence.entity.HospitalEntity;
import com.itesm.infrastructure.persistence.entity.SpecialtyEntity;
import com.itesm.infrastructure.persistence.entity.UserEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public List<User> listAllDomain() {
        return find("select distinct u from UserEntity u left join fetch u.roles r left join fetch r.privileges left join fetch u.hospital")
                .list()
                .stream()
                .map(UserMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> findByHospitalId(UUID hospitalId) {
        return find("select distinct u from UserEntity u left join fetch u.roles r left join fetch r.privileges left join fetch u.hospital where u.hospital.id = ?1", hospitalId)
                .list()
                .stream()
                .map(UserMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public User create(User user) {
        UserEntity entity = UserMapper.toEntity(user);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        if (user.getHospitalId() != null) {
            entity.setHospital(getEntityManager().getReference(HospitalEntity.class, user.getHospitalId()));
        }
        if (user.getSpecialtyId() != null) {
            entity.setSpecialty(getEntityManager().getReference(SpecialtyEntity.class, user.getSpecialtyId()));
        }
        persist(entity);
        getEntityManager().flush();
        getEntityManager().clear();
        return findUserById(entity.getId()).orElse(UserMapper.toDomain(entity));
    }

    @Override
    @Transactional
    public User update(User user) {
        UserEntity managed = find("select distinct u from UserEntity u left join fetch u.roles r left join fetch r.privileges left join fetch u.hospital where u.id = ?1", user.getId())
                .firstResult();
        managed.setFullName(user.getFullName());
        managed.setEmail(user.getEmail());
        managed.setActive(user.isActive());
        managed.setExternalAuthId(user.getExternalAuthId());
        managed.setStatus(user.getStatus());
        managed.setLastLoginAt(user.getLastLoginAt());
        managed.setLicenseNumber(user.getLicenseNumber());
        if (user.getHospitalId() != null) {
            managed.setHospital(getEntityManager().getReference(HospitalEntity.class, user.getHospitalId()));
        } else {
            managed.setHospital(null);
        }
        if (user.getSpecialtyId() != null) {
            managed.setSpecialty(getEntityManager().getReference(SpecialtyEntity.class, user.getSpecialtyId()));
        } else {
            managed.setSpecialty(null);
        }
        managed.setUpdatedAt(LocalDateTime.now());

        managed.getRoles().clear();
        managed.getRoles().addAll(UserMapper.toEntity(user).getRoles());

        persist(managed);
        getEntityManager().flush();
        getEntityManager().clear();
        return findUserById(managed.getId()).orElse(UserMapper.toDomain(managed));
    }

    @Override
    @Transactional
    public void updateLastLoginAt(UUID userId, LocalDateTime lastLoginAt) {
        update("lastLoginAt = ?1, updatedAt = ?2 where id = ?3",
                lastLoginAt, LocalDateTime.now(), userId);
    }

    private Optional<User> findWithRolesAndPrivileges(String field, Object value) {
        return find("select distinct u from UserEntity u left join fetch u.roles r left join fetch r.privileges left join fetch u.hospital where u." + field + " = ?1", value)
                .firstResultOptional()
                .map(UserMapper::toDomain);
    }
}

