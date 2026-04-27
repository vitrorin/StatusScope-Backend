package com.itesm.interfaces.rest;

import com.itesm.application.dto.RegisterUserDto;
import com.itesm.application.dto.UserSummaryDto;
import com.itesm.application.usecase.GetMyProfileUseCase;
import com.itesm.application.usecase.RegisterUserUseCase;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    RegisterUserUseCase registerUserUseCase;

    @Inject
    GetMyProfileUseCase getMyProfileUseCase;

    @POST
    @Path("/register")
    public Response register(@Valid RegisterUserDto dto) {
        UserSummaryDto created = registerUserUseCase.execute(dto);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @GET
    @Path("/me")
    public Response me() {
        UserSummaryDto profile = getMyProfileUseCase.execute();
        return Response.ok(profile).build();
    }
}

