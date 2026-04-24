package com.itesm.application.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

class CurrentUserTest {

    @Test
    void shouldCheckRoleAndPrivilege() {
        CurrentUser currentUser = new CurrentUser(
                UUID.randomUUID(),
                "ext-1",
                "test@local",
                "Test User",
                null,
                Set.of("ADMIN", "DOCTOR"),
                Set.of("roles.manage", "users.read")
        );

        Assertions.assertTrue(currentUser.hasRole("ADMIN"));
        Assertions.assertFalse(currentUser.hasRole("ANALYST"));
        Assertions.assertTrue(currentUser.hasPrivilege("roles.manage"));
        Assertions.assertFalse(currentUser.hasPrivilege("alerts.manage"));
    }
}
