package com.itesm.infrastructure.mapper;

import com.itesm.domain.models.Disease;
import com.itesm.domain.models.Municipality;
import com.itesm.domain.models.Outbreak;
import com.itesm.domain.models.State;
import com.itesm.infrastructure.persistence.entity.OutbreakEntity;
import com.itesm.infrastructure.persistence.entity.SymptomEntity;

import java.util.stream.Collectors;

public final class OutbreakMapper {
    private OutbreakMapper() {}

    public static Outbreak toDomain(OutbreakEntity e) {
        Outbreak o = new Outbreak();
        o.setId(e.getId());
        o.setScope(e.getScope());
        o.setCaseCount(e.getCaseCount());
        o.setConfirmationStatus(e.getConfirmationStatus());
        o.setStatus(e.getStatus());
        o.setStartedAt(e.getStartedAt());
        o.setEndedAt(e.getEndedAt());

        if (e.getDisease() != null) {
            Disease d = new Disease();
            d.setId(e.getDisease().getId());
            d.setCode(e.getDisease().getCode());
            d.setName(e.getDisease().getName());
            d.setSymptoms(e.getDisease().getSymptoms().stream()
                    .map(SymptomEntity::getName)
                    .sorted()
                    .collect(Collectors.joining(", ")));
            o.setDisease(d);
        }

        if (e.getMunicipality() != null) {
            Municipality municipality = MunicipalityMapper.toDomain(e.getMunicipality());
            o.setMunicipality(municipality);
        }

        if (e.getState() != null) {
            State state = new State();
            state.setId(e.getState().getId());
            state.setCode(e.getState().getCode());
            state.setName(e.getState().getName());
            state.setDescription(e.getState().getDescription());
            o.setState(state);
        } else if (e.getMunicipality() != null && e.getMunicipality().getState() != null) {
            State state = new State();
            state.setId(e.getMunicipality().getState().getId());
            state.setCode(e.getMunicipality().getState().getCode());
            state.setName(e.getMunicipality().getState().getName());
            state.setDescription(e.getMunicipality().getState().getDescription());
            o.setState(state);
        }

        return o;
    }
}
