package com.itesm.application.dto;

public class AssistantTranslationMessageDto {
    private String clientId;
    private String role;
    private String content;

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
