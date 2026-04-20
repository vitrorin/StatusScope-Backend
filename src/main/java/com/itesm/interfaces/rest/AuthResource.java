package com.itesm.interfaces.rest;

import com.itesm.application.dto.AuthResponseDto;
import com.itesm.application.dto.LoginDto;
import com.itesm.application.dto.MyProfileDto;
import com.itesm.application.dto.RegisterUserDto;
import com.itesm.application.security.RequiresPrivilege;
import com.itesm.application.usecase.GetMyProfileUseCase;
import com.itesm.application.usecase.LoginUseCase;
import com.itesm.application.usecase.RegisterUserUseCase;
import com.itesm.domain.models.User;
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
    LoginUseCase loginUseCase;

    @Inject
    GetMyProfileUseCase getMyProfileUseCase;

    @POST
    @Path("/register")
    public Response register(@Valid RegisterUserDto dto) {
        User user = registerUserUseCase.execute(dto);
        return Response.status(Response.Status.CREATED).entity(user).build();
    }

    @POST
    @Path("/login")
    public Response login(@Valid LoginDto dto) {
        String token = loginUseCase.execute(dto);
        return Response.ok(new AuthResponseDto(token)).build();
    }

    @GET
    @Path("/me")
    @RequiresPrivilege("users.read")
    public Response me() {
        MyProfileDto profile = getMyProfileUseCase.execute();
        return Response.ok(profile).build();
    }
}
