package com.itesm.infrastructure.llm;

import com.itesm.infrastructure.openai.dto.ChatMessage;

import java.util.List;

public interface LlmChatStrategy {
    String chat(List<ChatMessage> messages);
}
