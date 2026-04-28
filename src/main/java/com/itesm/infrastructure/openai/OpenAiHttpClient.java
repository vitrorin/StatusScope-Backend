package com.itesm.infrastructure.openai;

import com.itesm.infrastructure.openai.dto.ChatCompletionRequest;
import com.itesm.infrastructure.openai.dto.ChatCompletionResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "com.itesm.infrastructure.openai.OpenAiHttpClient")
@Path("/chat/completions")
public interface OpenAiHttpClient {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    ChatCompletionResponse complete(
            @HeaderParam("Authorization") String authorization,
            ChatCompletionRequest request);
}
