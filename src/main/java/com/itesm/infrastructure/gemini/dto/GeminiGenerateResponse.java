package com.itesm.infrastructure.gemini.dto;

import java.util.List;

public class GeminiGenerateResponse {
    private List<Candidate> candidates;

    public List<Candidate> getCandidates() { return candidates; }
    public void setCandidates(List<Candidate> candidates) { this.candidates = candidates; }

    public static class Candidate {
        private GeminiContent content;

        public GeminiContent getContent() { return content; }
        public void setContent(GeminiContent content) { this.content = content; }
    }
}
