package com.itesm.application.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;

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
}
