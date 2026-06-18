package com.proxio.apigateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Component
public class InternalTokenService {

    private final String secret;
    private final long ttlSeconds;

    public InternalTokenService(@Value("${gateway.internal-auth.secret:proxio-dev-internal-secret}") String secret,
                                @Value("${gateway.internal-auth.ttl-seconds:60}") long ttlSeconds) {
        this.secret = secret;
        this.ttlSeconds = ttlSeconds;
    }

    public String tokenFor(String serviceId) {
        long expiresAt = Instant.now().plusSeconds(ttlSeconds).getEpochSecond();
        String payload = serviceId + ":" + expiresAt;
        return base64(payload) + "." + base64(hmac(payload));
    }

    private byte[] hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not sign internal gateway token.", exception);
        }
    }

    private String base64(String value) {
        return base64(value.getBytes(StandardCharsets.UTF_8));
    }

    private String base64(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
