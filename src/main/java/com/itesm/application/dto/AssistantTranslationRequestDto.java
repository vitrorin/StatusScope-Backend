package com.itesm.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public class AssistantTranslationRequestDto {
    @NotBlank
    private String targetLanguage;

    @NotEmpty
    @Size(max = 20)
    @Valid
    private List<AssistantTranslationMessageDto> messages;

    public String getTargetLanguage() { return targetLanguage; }
    public void setTargetLanguage(String targetLanguage) { this.targetLanguage = targetLanguage; }

    public List<AssistantTranslationMessageDto> getMessages() { return messages; }
    public void setMessages(List<AssistantTranslationMessageDto> messages) { this.messages = messages; }
}
