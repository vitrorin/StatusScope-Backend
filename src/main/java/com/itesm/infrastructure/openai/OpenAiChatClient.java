package com.itesm.infrastructure.openai;

import com.itesm.application.usecase.exception.OpenAiException;
import com.itesm.infrastructure.llm.LlmChatStrategy;
import com.itesm.infrastructure.openai.dto.ChatCompletionRequest;
import com.itesm.infrastructure.openai.dto.ChatCompletionResponse;
import com.itesm.infrastructure.openai.dto.ChatMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
@Named("openai")
public class OpenAiChatClient implements LlmChatStrategy {

    private static final Logger LOG = Logger.getLogger(OpenAiChatClient.class);

    @ConfigProperty(name = "openai.api-key", defaultValue = "")
    String apiKey;

    @ConfigProperty(name = "openai.model", defaultValue = "gpt-4o")
    String model;

    @ConfigProperty(name = "openai.temperature", defaultValue = "0.2")
    double temperature;

    @RestClient
    OpenAiHttpClient httpClient;

    public OpenAiChatClient() {}

    public String chat(List<ChatMessage> messages) {
        if (apiKey == null || apiKey.isBlank()) {
            LOG.warn("OpenAI API key is not configured — diagnosis assistant calls will fail");
            throw new OpenAiException("OpenAI API key is not configured");
        }
        try {
            ChatCompletionRequest request = new ChatCompletionRequest(model, messages, temperature);
            ChatCompletionResponse response = httpClient.complete("Bearer " + apiKey, request);
            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new OpenAiException("Empty response from OpenAI");
            }
            return response.getChoices().get(0).getMessage().getContent();
        } catch (WebApplicationException e) {
            LOG.errorf(e, "OpenAI API error: %s", e.getMessage());
            throw new OpenAiException("OpenAI API returned an error: " + e.getResponse().getStatus());
        } catch (OpenAiException e) {
            throw e;
        } catch (Exception e) {
            LOG.errorf(e, "Unexpected error calling OpenAI: %s", e.getMessage());
            throw new OpenAiException("Unexpected error communicating with OpenAI");
        }
    }
}
