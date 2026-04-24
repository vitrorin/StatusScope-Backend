package com.itesm.interfaces.rest;

import com.itesm.application.security.RequiresPrivilege;
import com.itesm.domain.repository.RoleRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/admin")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AdminRoleResource {

    @Inject
    RoleRepository roleRepository;

    @GET
    @Path("/roles")
    @RequiresPrivilege("roles.manage")
    public Response listRoles() {
        return Response.ok(roleRepository.listAllRoles()).build();
    }
}

