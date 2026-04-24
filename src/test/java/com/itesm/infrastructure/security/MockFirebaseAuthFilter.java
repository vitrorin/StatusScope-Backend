package com.itesm.infrastructure.security;

import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.domain.models.Privilege;
import com.itesm.domain.models.Role;
import com.itesm.domain.models.User;
import com.itesm.domain.models.UserStatus;
import com.itesm.domain.repository.UserRepository;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@Provider
@Priority(Priorities.AUTHENTICATION)
@IfBuildProfile("test")
public class MockFirebaseAuthFilter implements ContainerRequestFilter {

    private static final Set<String> PUBLIC_PATH_PREFIXES = Set.of(
            "auth/register",
            "q/health",
            "q/openapi"
    );

    @Inject
    UserRepository userRepository;

    @Inject
    AuthenticatedUserContext authenticatedUserContext;

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        String path = ctx.getUriInfo().getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        final String normalizedPath = path;
        if (PUBLIC_PATH_PREFIXES.stream().anyMatch(normalizedPath::startsWith)) {
            return;
        }

        String authHeader = ctx.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ctx.abortWith(Response.status(401).build());
            return;
        }

        // Honor X-Test-User: <email> header to switch identities in tests
        String testUserEmail = ctx.getHeaders().getFirst("X-Test-User");
        if (testUserEmail != null && !testUserEmail.isBlank()) {
            User user = userRepository.findByEmail(testUserEmail.trim()).orElse(null);
            if (user == null || user.getStatus() != UserStatus.ACTIVE) {
                ctx.abortWith(Response.status(401).build());
                return;
            }
            populateContext(user);
            return;
        }

        // Default: deny (no real Firebase verification in tests; caller must specify X-Test-User)
        ctx.abortWith(Response.status(401).build());
    }

    private void populateContext(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(Role::getCode).collect(Collectors.toSet());
        Set<String> privileges = user.getRoles().stream()
                .flatMap(r -> r.getPrivileges().stream())
                .map(Privilege::getCode)
                .collect(Collectors.toSet());

        authenticatedUserContext.setCurrentUser(new CurrentUser(
                user.getId(),
                user.getExternalAuthId(),
                user.getEmail(),
                user.getFullName(),
                user.getHospitalId(),
                roles,
                privileges
        ));
    }
}
