package com.itesm.application.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AssistantResponseDto {
    private String reply;
    private AssistantContextDto contextUsed;
    private UUID messageId;
    private List<AssistantSuggestionDto> suggestions = new ArrayList<>();

    public AssistantResponseDto() {}

    public AssistantResponseDto(String reply, AssistantContextDto contextUsed) {
        this.reply = reply;
        this.contextUsed = contextUsed;
    }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public AssistantContextDto getContextUsed() { return contextUsed; }
    public void setContextUsed(AssistantContextDto contextUsed) { this.contextUsed = contextUsed; }

    public UUID getMessageId() { return messageId; }
    public void setMessageId(UUID messageId) { this.messageId = messageId; }

    public List<AssistantSuggestionDto> getSuggestions() { return suggestions; }
    public void setSuggestions(List<AssistantSuggestionDto> suggestions) { this.suggestions = suggestions; }
}
