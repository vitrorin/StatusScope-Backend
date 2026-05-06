package com.itesm.application.dto;

import java.time.LocalDateTime;

public class AssistantMessageDto {
    private String role;
    private String content;
    private LocalDateTime createdAt;

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
