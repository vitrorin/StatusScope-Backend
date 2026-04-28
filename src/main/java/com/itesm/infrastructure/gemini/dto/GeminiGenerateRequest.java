package com.itesm.infrastructure.gemini.dto;

import java.util.List;

public class GeminiGenerateRequest {
    private GeminiContent systemInstruction;
    private List<GeminiContent> contents;
    private GeminiGenerationConfig generationConfig;

    public GeminiContent getSystemInstruction() { return systemInstruction; }
    public void setSystemInstruction(GeminiContent systemInstruction) { this.systemInstruction = systemInstruction; }

    public List<GeminiContent> getContents() { return contents; }
    public void setContents(List<GeminiContent> contents) { this.contents = contents; }

    public GeminiGenerationConfig getGenerationConfig() { return generationConfig; }
    public void setGenerationConfig(GeminiGenerationConfig generationConfig) { this.generationConfig = generationConfig; }
}
