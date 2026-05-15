package com.itesm.application.dto;

public class AssistantResponseDto {
    private String reply;
    private java.util.Map<String, String> replyByLanguage;
    private AssistantContextDto contextUsed;

    public AssistantResponseDto() {}

    public AssistantResponseDto(String reply, AssistantContextDto contextUsed) {
        this.reply = reply;
        this.replyByLanguage = java.util.Map.of();
        this.contextUsed = contextUsed;
    }

    public AssistantResponseDto(String reply, java.util.Map<String, String> replyByLanguage, AssistantContextDto contextUsed) {
        this.reply = reply;
        this.replyByLanguage = replyByLanguage;
        this.contextUsed = contextUsed;
    }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public java.util.Map<String, String> getReplyByLanguage() { return replyByLanguage; }
    public void setReplyByLanguage(java.util.Map<String, String> replyByLanguage) { this.replyByLanguage = replyByLanguage; }

    public AssistantContextDto getContextUsed() { return contextUsed; }
    public void setContextUsed(AssistantContextDto contextUsed) { this.contextUsed = contextUsed; }
}
