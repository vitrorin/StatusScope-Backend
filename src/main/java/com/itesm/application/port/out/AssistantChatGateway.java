package com.itesm.application.port.out;

import java.util.List;

public interface AssistantChatGateway {
    String chat(List<AssistantChatMessage> messages);
}
