package com.itesm.infrastructure.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@ApplicationScoped
public class HmacTokenVerifier implements TokenVerifier {

    @ConfigProperty(name = "auth.jwt.secret")
    String secret;

    @ConfigProperty(name = "auth.jwt.issuer")
    String issuer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public TokenPayload verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid token format");
            }

            String signingInput = parts[0] + "." + parts[1];
            String expectedSignature = sign(signingInput);
            if (!constantTimeEquals(expectedSignature, parts[2])) {
                throw new IllegalArgumentException("Invalid token signature");
            }

            JsonNode payload = objectMapper.readTree(base64UrlDecode(parts[1]));
            String tokenIssuer = payload.path("iss").asText(null);
            long exp = payload.path("exp").asLong(0);
            String sub = payload.path("sub").asText(null);
            String email = payload.path("email").asText(null);

            if (tokenIssuer == null || !tokenIssuer.equals(issuer)) {
                throw new IllegalArgumentException("Invalid issuer");
            }
            if (sub == null || sub.isBlank()) {
                throw new IllegalArgumentException("Missing subject");
            }
            if (Instant.now().getEpochSecond() >= exp) {
                throw new IllegalArgumentException("Token expired");
            }

            return new TokenPayload(sub, email);
        } catch (Exception e) {
            throw new IllegalArgumentException("Token verification failed", e);
        }
    }

    private String sign(String input) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signatureBytes = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
    }

    private byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
