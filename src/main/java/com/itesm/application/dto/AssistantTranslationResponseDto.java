package com.itesm.application.dto;

import java.util.List;

public class AssistantTranslationResponseDto {
    private List<AssistantTranslationDto> translations;

    public AssistantTranslationResponseDto() {}

    public AssistantTranslationResponseDto(List<AssistantTranslationDto> translations) {
        this.translations = translations;
    }

    public List<AssistantTranslationDto> getTranslations() { return translations; }
    public void setTranslations(List<AssistantTranslationDto> translations) { this.translations = translations; }
}
