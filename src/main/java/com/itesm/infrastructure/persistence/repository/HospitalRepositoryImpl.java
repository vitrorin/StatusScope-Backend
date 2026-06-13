package com.itesm.infrastructure.persistence.repository;

import com.itesm.domain.models.Hospital;
import com.itesm.domain.repository.HospitalRepository;
import com.itesm.infrastructure.mapper.HospitalMapper;
import com.itesm.infrastructure.persistence.entity.HospitalEntity;
import com.itesm.infrastructure.persistence.entity.MunicipalityEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class HospitalRepositoryImpl implements HospitalRepository, PanacheRepositoryBase<HospitalEntity, UUID> {

    @Override
    public Optional<Hospital> findHospitalById(UUID id) {
        return findByIdOptional(id).map(HospitalMapper::toDomain);
    }

    @Override
    public Optional<Hospital> findByInviteCode(String inviteCode) {
        return find("inviteCode", inviteCode).firstResultOptional().map(HospitalMapper::toDomain);
    }

    @Override
    @Transactional
    public Hospital create(Hospital hospital) {
        HospitalEntity entity = HospitalMapper.toEntity(hospital);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        if (hospital.getMunicipalityId() != null) {
            entity.setMunicipality(getEntityManager().getReference(MunicipalityEntity.class, hospital.getMunicipalityId()));
        }
        persist(entity);
        return HospitalMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public Hospital update(Hospital hospital) {
        HospitalEntity managed = findByIdOptional(hospital.getId()).orElseThrow();
        managed.setCode(hospital.getCode());
        managed.setName(hospital.getName());
        managed.setAddress(hospital.getAddress());
        managed.setPhone(hospital.getPhone());
        managed.setInviteCode(hospital.getInviteCode());
        managed.setActive(hospital.isActive());
        managed.setPostalCode(hospital.getPostalCode());
        managed.setBedCount(hospital.getBedCount());
        managed.setDoctorCount(hospital.getDoctorCount());
        managed.setNurseCount(hospital.getNurseCount());
        managed.setLatitude(hospital.getLatitude());
        managed.setLongitude(hospital.getLongitude());
        managed.setUpdatedAt(LocalDateTime.now());
        if (hospital.getMunicipalityId() != null) {
            managed.setMunicipality(getEntityManager().getReference(MunicipalityEntity.class, hospital.getMunicipalityId()));
        } else {
            managed.setMunicipality(null);
        }
        persist(managed);
        getEntityManager().flush();
        getEntityManager().clear();
        return findHospitalById(hospital.getId()).orElse(HospitalMapper.toDomain(managed));
    }

    @Override
    public List<Hospital> listAllDomain() {
        return listAll().stream().map(HospitalMapper::toDomain).collect(Collectors.toList());
    }
}
