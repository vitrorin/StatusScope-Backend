package com.itesm.infrastructure.security;

public class TokenPayload {
    private String subject;
    private String email;

    public TokenPayload(String subject, String email) {
        this.subject = subject;
        this.email = email;
    }

    public String getSubject() {
        return subject;
    }

    public String getEmail() {
        return email;
    }
}
