package com.itesm.application.dto;

public class AssistantResponseDto {
    private String reply;
    private AssistantContextDto contextUsed;

    public AssistantResponseDto() {}

    public AssistantResponseDto(String reply, AssistantContextDto contextUsed) {
        this.reply = reply;
        this.contextUsed = contextUsed;
    }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public AssistantContextDto getContextUsed() { return contextUsed; }
    public void setContextUsed(AssistantContextDto contextUsed) { this.contextUsed = contextUsed; }
}
