package com.itesm.domain.repository;

import com.itesm.domain.models.Hospital;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HospitalRepository {
    Optional<Hospital> findHospitalById(UUID id);
    Optional<Hospital> findByInviteCode(String inviteCode);
    Hospital create(Hospital hospital);
    Hospital update(Hospital hospital);
    List<Hospital> listAllDomain();
}
