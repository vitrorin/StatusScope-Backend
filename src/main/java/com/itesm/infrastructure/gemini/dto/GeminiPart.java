package com.itesm.infrastructure.gemini.dto;

public class GeminiPart {
    private String text;

    public GeminiPart() {}

    public GeminiPart(String text) {
        this.text = text;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
