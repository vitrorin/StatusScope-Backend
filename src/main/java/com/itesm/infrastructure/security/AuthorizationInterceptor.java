package com.itesm.infrastructure.security;

import com.itesm.application.security.AuthenticatedUserContext;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.security.RequiresPrivilege;
import com.itesm.interfaces.rest.ApiError;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.ForbiddenException;
import org.jboss.logging.Logger;

@RequiresPrivilege("")
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class AuthorizationInterceptor {

    private static final Logger LOG = Logger.getLogger(AuthorizationInterceptor.class);

    @Inject
    AuthenticatedUserContext authenticatedUserContext;

    @AroundInvoke
    public Object authorize(InvocationContext context) throws Exception {
        RequiresPrivilege required = context.getMethod().getAnnotation(RequiresPrivilege.class);
        if (required == null) {
            required = context.getTarget().getClass().getAnnotation(RequiresPrivilege.class);
        }

        if (required == null || required.value().isBlank()) {
            return context.proceed();
        }

        CurrentUser currentUser = authenticatedUserContext.getCurrentUser();
        if (currentUser == null || !currentUser.hasPrivilege(required.value())) {
            String endpoint = context.getMethod().getDeclaringClass().getSimpleName() + "." + context.getMethod().getName();
            String userId = currentUser == null ? "anonymous" : currentUser.getUserId().toString();
            LOG.infof("AUDIT userId=%s endpoint=%s decision=DENY reason=missing_privilege:%s", userId, endpoint, required.value());
            throw new ForbiddenException("Missing privilege: " + required.value());
        }

        String endpoint = context.getMethod().getDeclaringClass().getSimpleName() + "." + context.getMethod().getName();
        LOG.infof("AUDIT userId=%s endpoint=%s decision=ALLOW", currentUser.getUserId(), endpoint);
        return context.proceed();
    }
}
