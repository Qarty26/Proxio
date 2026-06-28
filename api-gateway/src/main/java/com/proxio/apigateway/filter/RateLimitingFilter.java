package com.proxio.apigateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_WINDOW = 40;
    private static final long WINDOW_SECONDS = 60;

    private final Map<String, WindowCounter> requests = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = request.getRemoteAddr();
        Instant now = Instant.now();
        WindowCounter counter = requests.compute(key, (ignored, current) -> {
            if (current == null || now.isAfter(current.windowStart().plusSeconds(WINDOW_SECONDS))) {
                return new WindowCounter(now, 1);
            }
            return new WindowCounter(current.windowStart(), current.count() + 1);
        });

        if (counter.count() > MAX_REQUESTS_PER_WINDOW) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Rate limit exceeded. Try again in a minute.\"}");
            return;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_WINDOW));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(MAX_REQUESTS_PER_WINDOW - counter.count()));
        filterChain.doFilter(request, response);
    }

    private record WindowCounter(Instant windowStart, int count) {
    }
}
