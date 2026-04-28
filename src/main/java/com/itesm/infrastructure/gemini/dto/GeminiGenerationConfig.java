package com.itesm.infrastructure.gemini.dto;

public class GeminiGenerationConfig {
    private double temperature;

    public GeminiGenerationConfig() {}

    public GeminiGenerationConfig(double temperature) {
        this.temperature = temperature;
    }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
}
