package com.itesm.interfaces.rest;

import com.itesm.application.security.RequiresPrivilege;
import com.itesm.application.usecase.ListDiagnosisDiseasesUseCase;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/diagnosis/diseases")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DiagnosisDiseaseResource {

    @Inject
    ListDiagnosisDiseasesUseCase listDiagnosisDiseasesUseCase;

    @GET
    @RequiresPrivilege("diagnosis.assist")
    public Response list(@QueryParam("query") String query, @QueryParam("limit") Integer limit) {
        return Response.ok(listDiagnosisDiseasesUseCase.list(query, limit)).build();
    }
}
