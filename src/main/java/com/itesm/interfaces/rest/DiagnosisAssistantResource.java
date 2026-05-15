package com.itesm.interfaces.rest;

import com.itesm.application.dto.AssistantRequestDto;
import com.itesm.application.dto.AssistantContextDto;
import com.itesm.application.dto.AssistantMessageDto;
import com.itesm.application.dto.AssistantTranslationRequestDto;
import com.itesm.application.security.RequiresPrivilege;
import com.itesm.application.usecase.AskDiagnosisAssistantUseCase;
import com.itesm.application.usecase.TranslateDiagnosisAssistantMessagesUseCase;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Path("/diagnosis/assistant")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DiagnosisAssistantResource {

    @Inject
    AskDiagnosisAssistantUseCase useCase;

    @Inject
    TranslateDiagnosisAssistantMessagesUseCase translateUseCase;

    @POST
    @Path("/messages")
    @RequiresPrivilege("diagnosis.assist")
    public Response sendMessage(@Valid AssistantRequestDto dto) {
        return Response.ok(useCase.execute(dto)).build();
    }

    @POST
    @Path("/translations")
    @RequiresPrivilege("diagnosis.assist")
    public Response translateMessages(@Valid AssistantTranslationRequestDto dto) {
        return Response.ok(translateUseCase.execute(dto)).build();
    }

    @GET
    @Path("/evaluations/{evaluationId}/thread")
    @RequiresPrivilege("diagnosis.assist")
    public Response getThread(@PathParam("evaluationId") UUID evaluationId) {
        LocalDateTime now = LocalDateTime.now();
        return Response.ok(new AssistantThreadResponse(
                evaluationId.toString(),
                evaluationId.toString(),
                now,
                now,
                List.of(),
                null
        )).build();
    }

    public record AssistantThreadResponse(
            String id,
            String evaluationId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<AssistantMessageDto> messages,
            AssistantContextDto contextUsed
    ) {}
}
