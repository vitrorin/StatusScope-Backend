package com.itesm.interfaces.rest;

import com.itesm.application.dto.AssignRoleDto;
import com.itesm.application.security.RequiresPrivilege;
import com.itesm.application.usecase.AssignRoleToUserUseCase;
import com.itesm.domain.repository.RoleRepository;
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

import java.util.UUID;

@Path("/admin")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AdminRoleResource {

    @Inject
    AssignRoleToUserUseCase assignRoleToUserUseCase;

    @Inject
    RoleRepository roleRepository;

    @GET
    @Path("/roles")
    @RequiresPrivilege("roles.manage")
    public Response listRoles() {
        return Response.ok(roleRepository.listAllRoles()).build();
    }

    @POST
    @Path("/users/{userId}/roles")
    @RequiresPrivilege("roles.manage")
    public Response assignRole(@PathParam("userId") UUID userId, @Valid AssignRoleDto dto) {
        return Response.ok(assignRoleToUserUseCase.execute(dto.getRoleCode(), userId)).build();
    }
}
