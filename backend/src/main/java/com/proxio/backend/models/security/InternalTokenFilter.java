package com.proxio.backend.models.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

public class InternalTokenFilter extends OncePerRequestFilter {

    private final boolean enabled;
    private final String secret;

    public InternalTokenFilter(@Value("${proxio.internal-auth.enabled:false}") boolean enabled,
                               @Value("${proxio.internal-auth.secret:proxio-dev-internal-secret}") String secret) {
        this.enabled = enabled;
        this.secret = secret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!enabled || request.getRequestURI().startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader("X-Internal-Token");
        if (!isValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Missing or invalid internal service token.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValid(String token) {
        if (token == null || !token.contains(".")) {
            return false;
        }
        String[] parts = token.split("\\.", 2);
        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String[] fields = payload.split(":", 2);
            if (fields.length != 2 || Instant.now().getEpochSecond() > Long.parseLong(fields[1])) {
                return false;
            }
            String expected = Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(payload));
            return Objects.equals(expected, parts[1]);
        } catch (Exception exception) {
            return false;
        }
    }

    private byte[] hmac(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    }
}
