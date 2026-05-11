package com.itesm.infrastructure.llm;

import com.itesm.application.port.out.AssistantChatMessage;

import java.util.List;

public interface LlmChatStrategy {
    String chat(List<AssistantChatMessage> messages);
}
