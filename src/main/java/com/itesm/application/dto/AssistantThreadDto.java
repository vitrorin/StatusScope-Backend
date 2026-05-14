package com.itesm.application.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AssistantThreadDto {
    private UUID id;
    private UUID evaluationId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AssistantMessageDto> messages = new ArrayList<>();
    private AssistantContextDto contextUsed;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getEvaluationId() { return evaluationId; }
    public void setEvaluationId(UUID evaluationId) { this.evaluationId = evaluationId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<AssistantMessageDto> getMessages() { return messages; }
    public void setMessages(List<AssistantMessageDto> messages) { this.messages = messages; }

    public AssistantContextDto getContextUsed() { return contextUsed; }
    public void setContextUsed(AssistantContextDto contextUsed) { this.contextUsed = contextUsed; }
}
