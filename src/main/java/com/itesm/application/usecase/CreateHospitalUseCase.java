package com.itesm.application.usecase;

import com.itesm.application.dto.CreateHospitalDto;
import com.itesm.application.security.RequiresPrivilege;
import com.itesm.domain.models.Hospital;
import com.itesm.domain.repository.HospitalRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class CreateHospitalUseCase {

    @Inject
    HospitalRepository hospitalRepository;

    @RequiresPrivilege("hospitals.manage")
    public Hospital execute(CreateHospitalDto dto) {
        Hospital hospital = new Hospital();
        hospital.setId(UUID.randomUUID());
        hospital.setCode(dto.getCode());
        hospital.setName(dto.getName());
        hospital.setAddress(dto.getAddress());
        hospital.setPhone(dto.getPhone());
        hospital.setInviteCode(dto.getInviteCode());
        hospital.setPostalCode(dto.getPostalCode());
        hospital.setBedCount(dto.getBedCount());
        hospital.setDoctorCount(dto.getDoctorCount());
        hospital.setNurseCount(dto.getNurseCount());
        hospital.setMunicipalityId(dto.getMunicipalityId());
        hospital.setLatitude(dto.getLatitude());
        hospital.setLongitude(dto.getLongitude());
        hospital.setActive(true);
        return hospitalRepository.create(hospital);
    }
}
