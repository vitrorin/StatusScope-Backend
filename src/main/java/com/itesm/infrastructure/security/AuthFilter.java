package com.itesm.infrastructure.security;

import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.domain.models.User;
import com.itesm.domain.repository.UserRepository;
import com.itesm.interfaces.rest.ApiError;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(AuthFilter.class);

    @Inject
    UserRepository userRepository;

    @Inject
    TokenVerifier tokenVerifier;

    @Inject
    AuthenticatedUserContext authenticatedUserContext;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (isPublic(path)) {
            return;
        }

        String authHeader = requestContext.getHeaderString("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            deny(requestContext, "Missing bearer token");
            return;
        }

        try {
            String token = authHeader.substring("Bearer ".length());
            TokenPayload payload = tokenVerifier.verify(token);
            User user = userRepository.findByExternalAuthId(payload.getSubject()).orElse(null);
            if (user == null || !user.isActive()) {
                deny(requestContext, "Unknown or inactive user");
                return;
            }

            Set<String> roles = user.getRoles().stream().map(r -> r.getCode()).collect(Collectors.toSet());
            Set<String> privileges = user.getRoles().stream()
                    .flatMap(r -> r.getPrivileges().stream())
                    .map(p -> p.getCode())
                    .collect(Collectors.toSet());

            CurrentUser currentUser = new CurrentUser(
                    user.getId(),
                    user.getExternalAuthId(),
                    user.getEmail(),
                    user.getFullName(),
                    roles,
                    privileges
            );
            authenticatedUserContext.setCurrentUser(currentUser);
        } catch (RuntimeException ex) {
            deny(requestContext, ex.getMessage());
        }
    }

    private boolean isPublic(String path) {
        return path.equals("auth/register")
            || path.equals("auth/login")
                || path.startsWith("q/");
    }

    private void deny(ContainerRequestContext requestContext, String reason) {
        String endpoint = requestContext.getMethod() + " " + requestContext.getUriInfo().getPath();
        LOG.infof("AUDIT userId=anonymous endpoint=%s decision=DENY reason=%s", endpoint, reason);
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ApiError(401, "UNAUTHORIZED", "Authentication required"))
                .build());
    }
}
