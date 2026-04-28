package com.itesm.infrastructure.gemini;

import com.itesm.infrastructure.gemini.dto.GeminiGenerateRequest;
import com.itesm.infrastructure.gemini.dto.GeminiGenerateResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "com.itesm.infrastructure.gemini.GeminiHttpClient")
@Path("/models")
public interface GeminiHttpClient {

    @POST
    @Path("/{model}:generateContent")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    GeminiGenerateResponse generateContent(
            @PathParam("model") String model,
            @QueryParam("key") String apiKey,
            GeminiGenerateRequest request);
}
