package com.itesm.infrastructure.mapper;

import com.itesm.domain.models.Hospital;
import com.itesm.infrastructure.persistence.entity.HospitalEntity;

public final class HospitalMapper {
    private HospitalMapper() {}

    public static Hospital toDomain(HospitalEntity e) {
        Hospital h = new Hospital();
        h.setId(e.getId());
        h.setCode(e.getCode());
        h.setName(e.getName());
        h.setAddress(e.getAddress());
        h.setPhone(e.getPhone());
        h.setInviteCode(e.getInviteCode());
        h.setActive(e.isActive());
        h.setPostalCode(e.getPostalCode());
        h.setBedCount(e.getBedCount());
        h.setDoctorCount(e.getDoctorCount());
        h.setNurseCount(e.getNurseCount());
        h.setLatitude(e.getLatitude());
        h.setLongitude(e.getLongitude());
        if (e.getMunicipality() != null) {
            h.setMunicipalityId(e.getMunicipality().getId());
            h.setMunicipalityName(e.getMunicipality().getName());
            if (e.getMunicipality().getState() != null) {
                h.setStateId(e.getMunicipality().getState().getId());
                h.setStateName(e.getMunicipality().getState().getName());
            }
        }
        h.setCreatedAt(e.getCreatedAt());
        h.setUpdatedAt(e.getUpdatedAt());
        return h;
    }

    public static HospitalEntity toEntity(Hospital h) {
        HospitalEntity e = new HospitalEntity();
        e.setId(h.getId());
        e.setCode(h.getCode());
        e.setName(h.getName());
        e.setAddress(h.getAddress());
        e.setPhone(h.getPhone());
        e.setInviteCode(h.getInviteCode());
        e.setActive(h.isActive());
        e.setPostalCode(h.getPostalCode());
        e.setBedCount(h.getBedCount());
        e.setDoctorCount(h.getDoctorCount());
        e.setNurseCount(h.getNurseCount());
        e.setLatitude(h.getLatitude());
        e.setLongitude(h.getLongitude());
        e.setCreatedAt(h.getCreatedAt());
        e.setUpdatedAt(h.getUpdatedAt());
        return e;
    }
}
