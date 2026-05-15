package com.itesm.application.usecase;

import com.itesm.application.dto.AssistantTranslationDto;
import com.itesm.application.dto.AssistantTranslationMessageDto;
import com.itesm.application.dto.AssistantTranslationRequestDto;
import com.itesm.application.dto.AssistantTranslationResponseDto;
import com.itesm.application.port.out.AssistantChatMessage;
import com.itesm.infrastructure.llm.LlmChatClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class TranslateDiagnosisAssistantMessagesUseCase {

    @Inject
    LlmChatClient llmChatClient;

    public AssistantTranslationResponseDto execute(AssistantTranslationRequestDto request) {
        String targetLanguage = normalizeLanguage(request.getTargetLanguage());
        List<AssistantTranslationDto> translations = new ArrayList<>();

        for (AssistantTranslationMessageDto message : request.getMessages()) {
            String content = message.getContent() == null ? "" : message.getContent();
            if (content.isBlank()) {
                translations.add(new AssistantTranslationDto(message.getClientId(), content));
                continue;
            }

            translations.add(new AssistantTranslationDto(
                    message.getClientId(),
                    translate(content, targetLanguage)));
        }

        return new AssistantTranslationResponseDto(translations);
    }

    public String translate(String content, String targetLanguage) {
        if (content == null || content.isBlank()) {
            return content == null ? "" : content;
        }
        targetLanguage = normalizeLanguage(targetLanguage);
        String targetLabel = targetLanguage.equals("es") ? "Spanish for Mexico" : "English";
        List<AssistantChatMessage> messages = List.of(
                new AssistantChatMessage("system", """
                        You are a medical text translation assistant.
                        Translate only the provided text into %s.
                        Preserve the original clinical meaning exactly.
                        Do not add, remove, summarize, reinterpret, or correct medical content.
                        Do not change diagnoses, recommendations, severity, numbers, dates, disease names, medication names, lab test names, locations, or patient values.
                        Preserve markdown, line breaks, numbering, bullets, and emphasis as much as possible.
                        Return only the translated text.
                        """.formatted(targetLabel)),
                new AssistantChatMessage("user", content)
        );

        return llmChatClient.chat(messages);
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "en";
        }

        String normalized = language.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("es") ? "es" : "en";
    }
}
