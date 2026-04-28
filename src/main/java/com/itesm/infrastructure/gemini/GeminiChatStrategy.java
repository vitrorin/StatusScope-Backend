package com.itesm.infrastructure.gemini;

import com.itesm.application.usecase.exception.OpenAiException;
import com.itesm.infrastructure.gemini.dto.GeminiContent;
import com.itesm.infrastructure.gemini.dto.GeminiGenerateRequest;
import com.itesm.infrastructure.gemini.dto.GeminiGenerateResponse;
import com.itesm.infrastructure.gemini.dto.GeminiGenerationConfig;
import com.itesm.infrastructure.gemini.dto.GeminiPart;
import com.itesm.infrastructure.llm.LlmChatStrategy;
import com.itesm.infrastructure.openai.dto.ChatMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@Named("gemini")
public class GeminiChatStrategy implements LlmChatStrategy {

    private static final Logger LOG = Logger.getLogger(GeminiChatStrategy.class);

    @ConfigProperty(name = "gemini.api-key", defaultValue = "")
    String apiKey;

    @ConfigProperty(name = "gemini.model", defaultValue = "gemini-2.0-flash")
    String model;

    @ConfigProperty(name = "gemini.temperature", defaultValue = "0.2")
    double temperature;

    @RestClient
    GeminiHttpClient httpClient;

    @Override
    public String chat(List<ChatMessage> messages) {
        if (apiKey == null || apiKey.isBlank()) {
            LOG.warn("Gemini API key is not configured");
            throw new OpenAiException("Gemini API key is not configured");
        }
        try {
            GeminiGenerateRequest request = buildRequest(messages);
            GeminiGenerateResponse response = httpClient.generateContent(model, apiKey, request);

            if (response == null
                    || response.getCandidates() == null
                    || response.getCandidates().isEmpty()
                    || response.getCandidates().get(0).getContent() == null
                    || response.getCandidates().get(0).getContent().getParts() == null
                    || response.getCandidates().get(0).getContent().getParts().isEmpty()) {
                throw new OpenAiException("Empty response from Gemini");
            }

            return response.getCandidates().get(0).getContent().getParts().get(0).getText();

        } catch (WebApplicationException e) {
            LOG.errorf(e, "Gemini API error: %s", e.getMessage());
            throw new OpenAiException("Gemini API returned an error: " + e.getResponse().getStatus());
        } catch (OpenAiException e) {
            throw e;
        } catch (Exception e) {
            LOG.errorf(e, "Unexpected error calling Gemini: %s", e.getMessage());
            throw new OpenAiException("Unexpected error communicating with Gemini");
        }
    }

    private GeminiGenerateRequest buildRequest(List<ChatMessage> messages) {
        GeminiGenerateRequest request = new GeminiGenerateRequest();
        request.setGenerationConfig(new GeminiGenerationConfig(temperature));

        List<GeminiContent> contents = new ArrayList<>();

        for (ChatMessage msg : messages) {
            if ("system".equals(msg.getRole())) {
                // Gemini handles system messages via systemInstruction, not in contents
                GeminiContent systemInstruction = new GeminiContent(
                        null,
                        List.of(new GeminiPart(msg.getContent())));
                request.setSystemInstruction(systemInstruction);
            } else {
                // "user" stays "user"; "assistant" maps to "model" in Gemini
                String geminiRole = "assistant".equals(msg.getRole()) ? "model" : msg.getRole();
                contents.add(new GeminiContent(geminiRole, List.of(new GeminiPart(msg.getContent()))));
            }
        }

        request.setContents(contents);
        return request;
    }
}
