package com.itesm.infrastructure.openai.dto;

import java.util.List;

public class ChatCompletionRequest {
    private String model;
    private List<ChatMessage> messages;
    private double temperature;

    public ChatCompletionRequest(String model, List<ChatMessage> messages, double temperature) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature;
    }

    public String getModel() { return model; }
    public List<ChatMessage> getMessages() { return messages; }
    public double getTemperature() { return temperature; }
}
