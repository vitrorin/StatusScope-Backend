package com.itesm.infrastructure.security;

public interface TokenVerifier {
    TokenPayload verify(String token);
}
