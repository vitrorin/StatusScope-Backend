package com.itesm.infrastructure.openai;

import com.itesm.application.port.out.AssistantChatMessage;
import com.itesm.application.usecase.exception.OpenAiException;
import com.itesm.infrastructure.llm.LlmChatStrategy;
import com.itesm.infrastructure.openai.dto.ChatCompletionRequest;
import com.itesm.infrastructure.openai.dto.ChatCompletionResponse;
import com.itesm.infrastructure.openai.dto.ChatMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    public String chat(List<AssistantChatMessage> messages) {
        String key = apiKey.trim();
        if (key == null || key.isBlank()) {
            LOG.warn("OpenAI API key is not configured — diagnosis assistant calls will fail");
            throw new OpenAiException("OpenAI API key is not configured");
        }
        try {
            ChatCompletionRequest request = new ChatCompletionRequest(model, toProviderMessages(messages), temperature);
            ChatCompletionResponse response = httpClient.complete("Bearer " + key, request);
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

    private List<ChatMessage> toProviderMessages(List<AssistantChatMessage> messages) {
        return messages.stream()
                .map(message -> new ChatMessage(message.getRole(), message.getContent()))
                .collect(Collectors.toList());
    }
}
