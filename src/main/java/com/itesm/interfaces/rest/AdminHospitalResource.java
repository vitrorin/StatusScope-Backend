package com.itesm.interfaces.rest;

import com.itesm.application.dto.CreateHospitalDto;
import com.itesm.application.dto.UpdateHospitalStatusDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.RequiresPrivilege;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.application.usecase.CreateHospitalUseCase;
import com.itesm.domain.models.Hospital;
import com.itesm.domain.repository.HospitalRepository;
import com.itesm.domain.repository.MunicipalityRepository;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ForbiddenException;

import java.util.List;

@Path("/admin/hospitals")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AdminHospitalResource {

    @Inject
    CreateHospitalUseCase createHospitalUseCase;

    @Inject
    HospitalRepository hospitalRepository;

    @Inject
    MunicipalityRepository municipalityRepository;

    @Inject
    AuthenticatedUserContext authenticatedUserContext;

    @POST
    @RequiresPrivilege("isSystemAdmin")
    public Response create(@Valid CreateHospitalDto dto) {
        requireSystemAdmin();
        Hospital hospital = createHospitalUseCase.execute(dto);
        return Response.status(Response.Status.CREATED).entity(hospital).build();
    }

    @GET
    @RequiresPrivilege("isSystemAdmin")
    public Response list() {
        List<Hospital> hospitals = hospitalRepository.listAllDomain();
        return Response.ok(hospitals).build();
    }

    @GET
    @Path("/municipalities")
    @RequiresPrivilege("isSystemAdmin")
    public Response listMunicipalities() {
        requireSystemAdmin();
        return Response.ok(municipalityRepository.listAllDomain()).build();
    }

    @PUT
    @Path("/{id}")
    @RequiresPrivilege("isSystemAdmin")
    public Response update(@PathParam("id") java.util.UUID id, @Valid CreateHospitalDto dto) {
        requireSystemAdmin();
        Hospital hospital = hospitalRepository.findHospitalById(id)
                .orElseThrow(() -> new NotFoundException("Hospital not found"));
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
        return Response.ok(hospitalRepository.update(hospital)).build();
    }

    @PATCH
    @Path("/{id}/status")
    @RequiresPrivilege("isSystemAdmin")
    public Response updateStatus(@PathParam("id") java.util.UUID id, UpdateHospitalStatusDto dto) {
        requireSystemAdmin();
        Hospital hospital = hospitalRepository.findHospitalById(id)
                .orElseThrow(() -> new NotFoundException("Hospital not found"));
        hospital.setActive(dto != null && dto.getActive() != null ? dto.getActive() : !hospital.isActive());
        return Response.ok(hospitalRepository.update(hospital)).build();
    }

    private void requireSystemAdmin() {
        if (!authenticatedUserContext.getCurrentUser().isSystemAdmin()) {
            throw new ForbiddenException("Only system administrators can manage hospitals globally");
        }
    }
}
