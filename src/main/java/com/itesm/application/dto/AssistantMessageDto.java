package com.itesm.application.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AssistantMessageDto {
    private UUID id;
    private String role;
    private String content;
    private LocalDateTime createdAt;
    private List<AssistantSuggestionDto> suggestions = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<AssistantSuggestionDto> getSuggestions() { return suggestions; }
    public void setSuggestions(List<AssistantSuggestionDto> suggestions) { this.suggestions = suggestions; }
}
