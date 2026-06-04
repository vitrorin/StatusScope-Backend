package com.itesm.interfaces.rest;

import com.itesm.application.dto.AssignRoleDto;
import com.itesm.application.dto.AdminUserDto;
import com.itesm.application.dto.CreateUserByAdminDto;
import com.itesm.application.dto.UpdateUserByAdminDto;
import com.itesm.application.dto.UpdateUserStatusDto;
import com.itesm.application.dto.UserSummaryDto;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.AuthorizationService;
import com.itesm.application.security.RequiresPrivilege;
import com.itesm.application.usecase.AssignRoleToUserUseCase;
import com.itesm.application.usecase.CreateUserByAdminUseCase;
import com.itesm.application.usecase.DisableUserUseCase;
import com.itesm.application.usecase.UpdateUserByAdminUseCase;
import com.itesm.application.usecase.UpdateUserStatusByAdminUseCase;
import com.itesm.domain.models.User;
import com.itesm.domain.repository.HospitalRepository;
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
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
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
    UpdateUserByAdminUseCase updateUserByAdminUseCase;

    @Inject
    UpdateUserStatusByAdminUseCase updateUserStatusByAdminUseCase;

    @Inject
    AssignRoleToUserUseCase assignRoleToUserUseCase;

    @Inject
    UserRepository userRepository;

    @Inject
    HospitalRepository hospitalRepository;


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
        Map<UUID, String> hospitalNamesById = hospitalRepository.listAllDomain().stream()
                .collect(Collectors.toMap(h -> h.getId(), h -> h.getName()));
        List<AdminUserDto> response = users.stream()
                .map(user -> toAdminUserDto(user, hospitalNamesById))
                .collect(Collectors.toList());
        return Response.ok(response).build();
    }

    @PATCH
    @Path("/users/{id}/status")
    @RequiresPrivilege("users.manage")
    public Response updateUserStatus(@PathParam("id") UUID userId, UpdateUserStatusDto dto) {
        User updated = updateUserStatusByAdminUseCase.execute(userId, dto == null ? null : dto.getStatus());
        Map<UUID, String> hospitalNamesById = hospitalRepository.listAllDomain().stream()
                .collect(Collectors.toMap(h -> h.getId(), h -> h.getName()));
        return Response.ok(toAdminUserDto(updated, hospitalNamesById)).build();
    }

    @PUT
    @Path("/users/{id}")
    @RequiresPrivilege("users.manage")
    public Response updateUser(@PathParam("id") UUID userId, @Valid UpdateUserByAdminDto dto) {
        return Response.ok(updateUserByAdminUseCase.execute(userId, dto)).build();
    }

    @POST
    @Path("/users/{userId}/roles")
    @RequiresPrivilege("roles.manage")
    public Response assignRole(@PathParam("userId") UUID userId, @Valid AssignRoleDto dto) {
        User updated = assignRoleToUserUseCase.execute(dto.getRoleCode(), userId);
        Map<UUID, String> hospitalNamesById = hospitalRepository.listAllDomain().stream()
                .collect(Collectors.toMap(h -> h.getId(), h -> h.getName()));
        return Response.ok(toAdminUserDto(updated, hospitalNamesById)).build();
    }

    private AdminUserDto toAdminUserDto(User user, Map<UUID, String> hospitalNamesById) {
        AdminUserDto dto = new AdminUserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setHospitalId(user.getHospitalId());
        dto.setHospitalName(user.getHospitalId() == null ? null : hospitalNamesById.get(user.getHospitalId()));
        dto.setStatus(user.getStatus() == null ? null : user.getStatus().name());
        dto.setRoleCodes(user.getRoles().stream().map(role -> role.getCode()).collect(Collectors.toSet()));
        return dto;
    }

}
