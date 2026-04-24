package com.itesm.application.security;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

class AuthorizationServiceTest {

    private AuthorizationService authorizationService;
    private AuthenticatedUserContext context;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService();
        context = new AuthenticatedUserContext();
        authorizationService.authenticatedUserContext = context;
    }

    @Test
    void shouldThrowUnauthorizedWhenNoUserInContext() {
        Assertions.assertThrows(NotAuthorizedException.class, () -> authorizationService.assertHasPrivilege("users.read"));
    }

    @Test
    void shouldThrowForbiddenWhenMissingPrivilege() {
        context.setCurrentUser(new CurrentUser(
                UUID.randomUUID(),
                "ext-2",
                "x@local",
                "X",
                null,
                Set.of("DOCTOR"),
                Set.of("alerts.read")
        ));

        Assertions.assertThrows(ForbiddenException.class, () -> authorizationService.assertHasPrivilege("roles.manage"));
    }

    @Test
    void shouldPassWhenPrivilegePresent() {
        context.setCurrentUser(new CurrentUser(
                UUID.randomUUID(),
                "ext-3",
                "y@local",
                "Y",
                null,
                Set.of("ADMIN"),
                Set.of("roles.manage")
        ));

        Assertions.assertDoesNotThrow(() -> authorizationService.assertHasPrivilege("roles.manage"));
    }
}
