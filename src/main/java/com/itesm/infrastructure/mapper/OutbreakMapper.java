package com.itesm.infrastructure.mapper;

import com.itesm.domain.models.Disease;
import com.itesm.domain.models.Municipality;
import com.itesm.domain.models.Outbreak;
import com.itesm.domain.models.State;
import com.itesm.infrastructure.persistence.entity.OutbreakEntity;

public final class OutbreakMapper {
    private OutbreakMapper() {}

    public static Outbreak toDomain(OutbreakEntity e) {
        Outbreak o = new Outbreak();
        o.setId(e.getId());
        o.setCaseCount(e.getCaseCount());
        o.setStatus(e.getStatus());
        o.setStartedAt(e.getStartedAt());
        o.setEndedAt(e.getEndedAt());

        if (e.getDisease() != null) {
            Disease d = new Disease();
            d.setId(e.getDisease().getId());
            d.setCode(e.getDisease().getCode());
            d.setName(e.getDisease().getName());
            d.setSymptoms(e.getDisease().getSymptoms());
            o.setDisease(d);
        }

        if (e.getMunicipality() != null) {
            Municipality municipality = MunicipalityMapper.toDomain(e.getMunicipality());
            o.setMunicipality(municipality);
        }

        if (e.getMunicipality() != null
                && e.getMunicipality().getCity() != null
                && e.getMunicipality().getCity().getState() != null) {
            State state = new State();
            state.setId(e.getMunicipality().getCity().getState().getId());
            state.setCode(e.getMunicipality().getCity().getState().getCode());
            state.setName(e.getMunicipality().getCity().getState().getName());
            state.setDescription(e.getMunicipality().getCity().getState().getDescription());
            o.setState(state);
        }

        return o;
    }
}
