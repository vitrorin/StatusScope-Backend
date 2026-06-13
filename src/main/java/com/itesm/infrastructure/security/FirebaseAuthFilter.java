package com.itesm.infrastructure.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.domain.models.Privilege;
import com.itesm.domain.models.Role;
import com.itesm.domain.models.User;
import com.itesm.domain.models.UserStatus;
import com.itesm.domain.repository.UserRepository;
import io.quarkus.arc.profile.UnlessBuildProfile;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Provider
@Priority(Priorities.AUTHENTICATION)
@UnlessBuildProfile("test")
public class FirebaseAuthFilter implements ContainerRequestFilter {

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
        if (HttpMethod.OPTIONS.equalsIgnoreCase(ctx.getMethod())) {
            return;
        }

        String path = ctx.getUriInfo().getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        final String normalizedPath = path;
        if (PUBLIC_PATH_PREFIXES.stream().anyMatch(normalizedPath::startsWith)) {
            return;
        }

        String header = ctx.getHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            abortUnauthorized(ctx);
            return;
        }

        try {
            String token = header.substring("Bearer ".length()).strip();
            if (token.isEmpty()) {
                abortUnauthorized(ctx);
                return;
            }
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(token, true);
            String uid = decoded.getUid();
            if (uid == null || uid.isBlank()) {
                abortUnauthorized(ctx);
                return;
            }

            User user = userRepository.findByExternalAuthId(uid).orElse(null);
            if (user == null || user.getStatus() != UserStatus.ACTIVE) {
                abortUnauthorized(ctx);
                return;
            }

            Set<String> roles = user.getRoles().stream()
                    .map(Role::getCode).collect(Collectors.toSet());
            Set<String> privileges = user.getRoles().stream()
                    .flatMap(r -> r.getPrivileges().stream())
                    .map(Privilege::getCode)
                    .collect(Collectors.toSet());

            authenticatedUserContext.setCurrentUser(new CurrentUser(
                    user.getId(),
                    decoded.getUid(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getHospitalId(),
                    roles,
                    privileges
            ));
        } catch (FirebaseAuthException e) {
            abortUnauthorized(ctx);
        }
    }

    private void abortUnauthorized(ContainerRequestContext ctx) {
        ctx.abortWith(Response.status(401).build());
    }
}
