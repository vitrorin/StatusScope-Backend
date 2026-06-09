package com.itesm.interfaces.rest;

import com.itesm.application.security.RequiresPrivilege;
import com.itesm.application.usecase.GetSystemDashboardSummaryUseCase;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/system/dashboard")
@Produces(MediaType.APPLICATION_JSON)
public class SystemDashboardResource {

    @Inject
    GetSystemDashboardSummaryUseCase getSystemDashboardSummaryUseCase;

    @GET
    @Path("/summary")
    @RequiresPrivilege("isSystemAdmin")
    public Response summary() {
        return Response.ok(getSystemDashboardSummaryUseCase.execute()).build();
    }
}
