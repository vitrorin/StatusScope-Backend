package com.itesm.infrastructure.mapper;

import com.itesm.domain.models.Disease;
import com.itesm.domain.models.Outbreak;
import com.itesm.domain.models.Region;
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

        if (e.getRegion() != null) {
            Region r = new Region();
            r.setId(e.getRegion().getId());
            r.setCode(e.getRegion().getCode());
            r.setName(e.getRegion().getName());
            r.setDescription(e.getRegion().getDescription());
            o.setRegion(r);
        }

        return o;
    }
}
