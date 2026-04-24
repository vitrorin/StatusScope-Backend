package com.itesm.interfaces.rest;

import com.itesm.application.dto.CreateHospitalDto;
import com.itesm.application.security.RequiresPrivilege;
import com.itesm.application.usecase.CreateHospitalUseCase;
import com.itesm.domain.models.Hospital;
import com.itesm.domain.repository.HospitalRepository;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/admin/hospitals")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AdminHospitalResource {

    @Inject
    CreateHospitalUseCase createHospitalUseCase;

    @Inject
    HospitalRepository hospitalRepository;

    @POST
    @RequiresPrivilege("hospitals.manage")
    public Response create(@Valid CreateHospitalDto dto) {
        Hospital hospital = createHospitalUseCase.execute(dto);
        return Response.status(Response.Status.CREATED).entity(hospital).build();
    }

    @GET
    @RequiresPrivilege("hospitals.read")
    public Response list() {
        List<Hospital> hospitals = hospitalRepository.listAllDomain();
        return Response.ok(hospitals).build();
    }
}
