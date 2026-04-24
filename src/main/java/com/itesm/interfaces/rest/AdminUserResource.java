package com.itesm.interfaces.rest;

import com.itesm.application.dto.AssignRoleDto;
import com.itesm.application.dto.CreateUserByAdminDto;
import com.itesm.application.dto.UserSummaryDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.AuthorizationService;
import com.itesm.application.security.RequiresPrivilege;
import com.itesm.application.usecase.AssignRoleToUserUseCase;
import com.itesm.application.usecase.CreateUserByAdminUseCase;
import com.itesm.application.usecase.DisableUserUseCase;
import com.itesm.domain.models.User;
import com.itesm.domain.repository.UserRepository;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/admin")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AdminUserResource {

    @Inject
    CreateUserByAdminUseCase createUserByAdminUseCase;

    @Inject
    DisableUserUseCase disableUserUseCase;

    @Inject
    AssignRoleToUserUseCase assignRoleToUserUseCase;

    @Inject
    UserRepository userRepository;


    @Inject
    AuthenticatedUserContext authenticatedUserContext;

    @Inject
    AuthorizationService authorizationService;

    @POST
    @Path("/users")
    @RequiresPrivilege("users.manage")
    public Response createUser(@Valid CreateUserByAdminDto dto) {
        UserSummaryDto created = createUserByAdminUseCase.execute(dto);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @GET
    @Path("/users")
    @RequiresPrivilege("users.read")
    public Response listUsers() {
        var caller = authenticatedUserContext.getCurrentUser();
        List<User> users;
        if (caller.isSystemAdmin()) {
            users = userRepository.listAllDomain();
        } else {
            authorizationService.assertSameHospital(caller.getHospitalId());
            users = userRepository.findByHospitalId(caller.getHospitalId());
        }
        return Response.ok(users).build();
    }

    @PATCH
    @Path("/users/{id}/status")
    @RequiresPrivilege("users.manage")
    public Response disableUser(@PathParam("id") UUID userId) {
        disableUserUseCase.execute(userId);
        return Response.noContent().build();
    }

    @POST
    @Path("/users/{userId}/roles")
    @RequiresPrivilege("roles.manage")
    public Response assignRole(@PathParam("userId") UUID userId, @Valid AssignRoleDto dto) {
        return Response.ok(assignRoleToUserUseCase.execute(dto.getRoleCode(), userId)).build();
    }

}
