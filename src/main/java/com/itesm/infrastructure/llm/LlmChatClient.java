package com.itesm.infrastructure.llm;

import com.itesm.application.port.out.AssistantChatGateway;
import com.itesm.application.port.out.AssistantChatMessage;
import com.itesm.application.usecase.exception.OpenAiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Orchestrates LLM calls using the Strategy pattern.
 * Tries the primary strategy (OpenAI GPT-4o) first; on failure, falls back to the
 * secondary strategy (Gemini). If both fail, the secondary exception is propagated.
 */
@ApplicationScoped
public class LlmChatClient implements AssistantChatGateway {

    private static final Logger LOG = Logger.getLogger(LlmChatClient.class);

    @Inject
    @Named("openai")
    LlmChatStrategy primary;

    @Inject
    @Named("gemini")
    LlmChatStrategy fallback;

    @Override
    public String chat(List<AssistantChatMessage> messages) {
        try {
            return primary.chat(messages);
        } catch (Exception primaryException) {
            LOG.warnf("Primary LLM (OpenAI) failed: %s — falling back to Gemini", primaryException.getMessage());
            try {
                return fallback.chat(messages);
            } catch (Exception fallbackException) {
                LOG.errorf("Fallback LLM (Gemini) also failed: %s", fallbackException.getMessage());
                throw new OpenAiException(
                        "All LLM providers failed. Primary: " + primaryException.getMessage()
                                + ". Fallback: " + fallbackException.getMessage());
            }
        }
    }
}
