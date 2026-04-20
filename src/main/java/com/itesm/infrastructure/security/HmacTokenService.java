package com.itesm.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itesm.application.security.CurrentUser;
import com.itesm.application.security.TokenService;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
public class HmacTokenService implements TokenService {

    @ConfigProperty(name = "auth.jwt.secret")
    String secret;

    @ConfigProperty(name = "auth.jwt.issuer")
    String issuer;

    @ConfigProperty(name = "auth.jwt.expiration-seconds")
    long expirationSeconds;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String issueToken(CurrentUser currentUser) {
        try {
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            Instant now = Instant.now();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", currentUser.getExternalAuthId());
            payload.put("email", currentUser.getEmail());
            payload.put("iss", issuer);
            payload.put("iat", now.getEpochSecond());
            payload.put("exp", now.plusSeconds(expirationSeconds).getEpochSecond());

            String encodedHeader = base64UrlEncode(objectMapper.writeValueAsBytes(header));
            String encodedPayload = base64UrlEncode(objectMapper.writeValueAsBytes(payload));
            String signingInput = encodedHeader + "." + encodedPayload;
            String signature = sign(signingInput);
            return signingInput + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Could not issue token", e);
        }
    }

    private String sign(String input) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signatureBytes = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        return base64UrlEncode(signatureBytes);
    }

    private String base64UrlEncode(byte[] input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input);
    }
}
