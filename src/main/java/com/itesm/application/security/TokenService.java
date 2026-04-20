package com.itesm.application.security;

public interface TokenService {
    String issueToken(CurrentUser currentUser);
}
