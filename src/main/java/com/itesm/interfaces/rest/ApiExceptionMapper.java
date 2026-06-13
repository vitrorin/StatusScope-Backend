package com.itesm.interfaces.rest;

import com.google.firebase.auth.FirebaseAuthException;
import com.itesm.application.usecase.exception.ConflictException;
import com.itesm.application.usecase.exception.InvalidInviteException;
import com.itesm.application.usecase.exception.NotFoundException;
import com.itesm.application.usecase.exception.OpenAiException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(ApiExceptionMapper.class);
    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof ConflictException) {
            return Response.status(Response.Status.CONFLICT)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ApiError(409, "CONFLICT", exception.getMessage()))
                    .build();
        }
        if (exception instanceof NotFoundException) {
            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ApiError(404, "NOT_FOUND", exception.getMessage()))
                    .build();
        }
        if (exception instanceof jakarta.ws.rs.NotFoundException) {
            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ApiError(404, "NOT_FOUND", "Resource not found"))
                    .build();
        }
        if (exception instanceof InvalidInviteException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ApiError(400, "INVALID_INVITE", exception.getMessage()))
                    .build();
        }
        if (exception instanceof NotAuthorizedException) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ApiError(401, "UNAUTHORIZED", "Authentication required"))
                    .build();
        }
        if (exception instanceof ForbiddenException) {
            return Response.status(Response.Status.FORBIDDEN)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ApiError(403, "FORBIDDEN", "Access denied"))
                    .build();
        }
        if (exception instanceof ConstraintViolationException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ApiError(400, "BAD_REQUEST", "Invalid request payload"))
                    .build();
        }
        if (exception instanceof IllegalArgumentException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ApiError(400, "BAD_REQUEST", exception.getMessage()))
                    .build();
        }
        if (exception instanceof FirebaseAuthException) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ApiError(500, "FIREBASE_ERROR", "Firebase operation failed"))
                    .build();
        }
        if (exception instanceof OpenAiException) {
            return Response.status(502)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ApiError(502, "ASSISTANT_UNAVAILABLE", exception.getMessage()))
                    .build();
        }

        LOG.errorf(exception, "Unhandled exception: %s", exception.getMessage());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ApiError(500, "INTERNAL_ERROR", "Unexpected server error"))
                .build();
    }
}
