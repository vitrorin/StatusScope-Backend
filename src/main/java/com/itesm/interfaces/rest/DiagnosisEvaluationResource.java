package com.itesm.interfaces.rest;

import com.itesm.application.dto.UpdateDiagnosisEvaluationDto;
import com.itesm.application.dto.UpdateDiagnosisEvaluationStatusDto;
import com.itesm.application.dto.UploadDiagnosisEvaluationFileDto;
import com.itesm.application.security.RequiresPrivilege;
import com.itesm.application.usecase.CreateDiagnosisEvaluationUseCase;
import com.itesm.application.usecase.GetDiagnosisEvaluationUseCase;
import com.itesm.application.usecase.UpdateDiagnosisEvaluationUseCase;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.validation.Valid;

import java.util.UUID;

@Path("/diagnosis/evaluations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DiagnosisEvaluationResource {

    @Inject
    GetDiagnosisEvaluationUseCase getDiagnosisEvaluationUseCase;

    @Inject
    CreateDiagnosisEvaluationUseCase createDiagnosisEvaluationUseCase;

    @Inject
    UpdateDiagnosisEvaluationUseCase updateDiagnosisEvaluationUseCase;

    @POST
    @RequiresPrivilege("diagnosis.assist")
    public Response create(@Valid UpdateDiagnosisEvaluationDto dto) {
        return Response.ok(createDiagnosisEvaluationUseCase.create(dto)).build();
    }

    @GET
    @Path("/current")
    @RequiresPrivilege("diagnosis.assist")
    public Response current() {
        return Response.ok(getDiagnosisEvaluationUseCase.current()).build();
    }

    @GET
    @Path("/{id}")
    @RequiresPrivilege("diagnosis.assist")
    public Response byId(@PathParam("id") UUID id) {
        return Response.ok(getDiagnosisEvaluationUseCase.byId(id)).build();
    }

    @PUT
    @Path("/{id}")
    @RequiresPrivilege("diagnosis.assist")
    public Response update(@PathParam("id") UUID id, @Valid UpdateDiagnosisEvaluationDto dto) {
        return Response.ok(updateDiagnosisEvaluationUseCase.update(id, dto)).build();
    }

    @POST
    @Path("/{id}/status")
    @RequiresPrivilege("diagnosis.assist")
    public Response updateStatus(@PathParam("id") UUID id, @Valid UpdateDiagnosisEvaluationStatusDto dto) {
        return Response.ok(updateDiagnosisEvaluationUseCase.updateStatus(id, dto.getStatus())).build();
    }

    @POST
    @Path("/{id}/files")
    @RequiresPrivilege("diagnosis.assist")
    public Response uploadFile(@PathParam("id") UUID id, @Valid UploadDiagnosisEvaluationFileDto dto) {
        return Response.ok(updateDiagnosisEvaluationUseCase.uploadFile(id, dto)).build();
    }
}
