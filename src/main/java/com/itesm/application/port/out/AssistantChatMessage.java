package com.itesm.application.port.out;

public class AssistantChatMessage {
    private final String role;
    private final String content;

    public AssistantChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }
}
