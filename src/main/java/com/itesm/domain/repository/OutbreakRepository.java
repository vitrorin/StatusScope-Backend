package com.itesm.domain.repository;

import com.itesm.domain.models.Outbreak;

import java.util.List;
import java.util.UUID;

public interface OutbreakRepository {
    List<Outbreak> findActiveByMunicipalityIds(List<UUID> municipalityIds);
    List<Outbreak> findActiveByMunicipalityIdsOrStateId(List<UUID> municipalityIds, UUID stateId);
}
