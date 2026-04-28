package com.itesm.interfaces.rest;

import com.itesm.application.dto.AssistantRequestDto;
import com.itesm.application.security.RequiresPrivilege;
import com.itesm.application.usecase.AskDiagnosisAssistantUseCase;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/diagnosis/assistant")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DiagnosisAssistantResource {

    @Inject
    AskDiagnosisAssistantUseCase useCase;

    @POST
    @Path("/messages")
    @RequiresPrivilege("diagnosis.assist")
    public Response sendMessage(@Valid AssistantRequestDto dto) {
        return Response.ok(useCase.execute(dto)).build();
    }
}
