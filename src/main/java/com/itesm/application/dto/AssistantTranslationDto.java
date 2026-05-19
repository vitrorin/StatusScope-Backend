package com.itesm.application.dto;

public class AssistantTranslationDto {
    private String clientId;
    private String content;

    public AssistantTranslationDto() {}

    public AssistantTranslationDto(String clientId, String content) {
        this.clientId = clientId;
        this.content = content;
    }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
