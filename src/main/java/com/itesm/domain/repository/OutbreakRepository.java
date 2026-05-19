package com.itesm.domain.repository;

import com.itesm.domain.models.Outbreak;

import java.util.List;
import java.util.UUID;

public interface OutbreakRepository {
    List<Outbreak> findAllActiveMunicipal();
    List<Outbreak> findActiveMunicipalByStateId(UUID stateId);
    List<Outbreak> findActiveStateByStateId(UUID stateId);
    List<Outbreak> findActiveByMunicipalityIds(List<UUID> municipalityIds);
    List<Outbreak> findActiveByMunicipalityIdsOrStateId(List<UUID> municipalityIds, UUID stateId);
}
