package com.itesm.application.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;

import java.util.UUID;

@ApplicationScoped
public class AuthorizationService {

    @Inject
    AuthenticatedUserContext authenticatedUserContext;

    public void assertHasPrivilege(String privilegeCode) {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        if (currentUser == null) {
            throw new NotAuthorizedException("Unauthorized");
        }
        if (!currentUser.hasPrivilege(privilegeCode)) {
            throw new ForbiddenException("Missing required privilege: " + privilegeCode);
        }
    }

    public void assertSameHospital(UUID targetHospitalId) {
        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        if (currentUser == null) {
            throw new NotAuthorizedException("Unauthorized");
        }
        if (currentUser.isSystemAdmin()) {
            return;
        }
        if (targetHospitalId == null || !targetHospitalId.equals(currentUser.getHospitalId())) {
            throw new ForbiddenException("Access denied: hospital scope mismatch");
        }
    }
}

