package com.itesm.infrastructure.llm;

import com.itesm.application.port.out.AssistantChatMessage;
import com.itesm.application.usecase.exception.OpenAiException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

class LlmChatClientTest {

    private LlmChatClient client;
    private LlmChatStrategy primary;
    private LlmChatStrategy fallback;

    @BeforeEach
    void setUp() {
        client = new LlmChatClient();
        primary = Mockito.mock(LlmChatStrategy.class);
        fallback = Mockito.mock(LlmChatStrategy.class);
        client.primary = primary;
        client.fallback = fallback;
    }

    @Test
    void shouldUsePrimaryProviderWhenItSucceeds() {
        List<AssistantChatMessage> messages = List.of(new AssistantChatMessage("user", "hello"));
        Mockito.when(primary.chat(messages)).thenReturn("primary reply");

        String reply = client.chat(messages);

        Assertions.assertEquals("primary reply", reply);
        Mockito.verify(primary).chat(messages);
        Mockito.verifyNoInteractions(fallback);
    }

    @Test
    void shouldFallbackWhenPrimaryProviderFails() {
        List<AssistantChatMessage> messages = List.of(new AssistantChatMessage("user", "hello"));
        Mockito.when(primary.chat(messages)).thenThrow(new RuntimeException("primary down"));
        Mockito.when(fallback.chat(messages)).thenReturn("fallback reply");

        String reply = client.chat(messages);

        Assertions.assertEquals("fallback reply", reply);
        Mockito.verify(primary).chat(messages);
        Mockito.verify(fallback).chat(messages);
    }

    @Test
    void shouldThrowCombinedExceptionWhenAllProvidersFail() {
        List<AssistantChatMessage> messages = List.of(new AssistantChatMessage("user", "hello"));
        Mockito.when(primary.chat(messages)).thenThrow(new RuntimeException("primary down"));
        Mockito.when(fallback.chat(messages)).thenThrow(new RuntimeException("fallback down"));

        OpenAiException exception = Assertions.assertThrows(OpenAiException.class, () -> client.chat(messages));

        Assertions.assertTrue(exception.getMessage().contains("primary down"));
        Assertions.assertTrue(exception.getMessage().contains("fallback down"));
        Mockito.verify(primary).chat(messages);
        Mockito.verify(fallback).chat(messages);
    }
}
