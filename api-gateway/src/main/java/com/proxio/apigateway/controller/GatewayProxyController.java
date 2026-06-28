package com.proxio.apigateway.controller;

import com.proxio.apigateway.discovery.TargetResolver;
import com.proxio.apigateway.resilience.CircuitBreakerRegistry;
import com.proxio.apigateway.security.InternalTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@RestController
public class GatewayProxyController {

    private final HttpClient httpClient;
    private final TargetResolver targetResolver;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final InternalTokenService internalTokenService;
    private final int maxAttempts;
    private final long requestTimeoutMs;

    public GatewayProxyController(HttpClient httpClient,
                                  TargetResolver targetResolver,
                                  CircuitBreakerRegistry circuitBreakerRegistry,
                                  InternalTokenService internalTokenService,
                                  @Value("${gateway.resilience.max-attempts:2}") int maxAttempts,
                                  @Value("${gateway.resilience.request-timeout-ms:2500}") long requestTimeoutMs) {
        this.httpClient = httpClient;
        this.targetResolver = targetResolver;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.internalTokenService = internalTokenService;
        this.maxAttempts = maxAttempts;
        this.requestTimeoutMs = requestTimeoutMs;
    }

    @RequestMapping({
            "/api/**",
            "/logout"
    })
    public ResponseEntity<byte[]> proxy(HttpServletRequest request) throws IOException {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String serviceId = resolveServiceId(uri);
        if (!circuitBreakerRegistry.allowsRequest(serviceId)) {
            return fallbackResponse(serviceId, 0, null, null);
        }

        byte[] requestBody = StreamUtils.copyToByteArray(request.getInputStream());

        IOException lastIOException = null;
        InterruptedException lastInterruptedException = null;
        int attempts = 0;

        for (String targetBase : targetResolver.candidates(serviceId)) {
            if (attempts >= maxAttempts) {
                break;
            }
            attempts++;
            String target = targetBase + uri + (query == null ? "" : "?" + query);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(target))
                    .timeout(Duration.ofMillis(requestTimeoutMs))
                    .method(request.getMethod(), requestBody.length == 0
                            ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofByteArray(requestBody));

            copyRequestHeaders(request, builder);
            builder.header("X-Internal-Token", internalTokenService.tokenFor(serviceId));

            try {
                HttpResponse<byte[]> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() >= 500 && attempts < maxAttempts) {
                    circuitBreakerRegistry.recordFailure(serviceId);
                    continue;
                }
                if (response.statusCode() >= 500) {
                    circuitBreakerRegistry.recordFailure(serviceId);
                } else {
                    circuitBreakerRegistry.recordSuccess(serviceId);
                }
                return proxiedResponse(response, targetBase, serviceId);
            } catch (IOException exception) {
                circuitBreakerRegistry.recordFailure(serviceId);
                lastIOException = exception;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                circuitBreakerRegistry.recordFailure(serviceId);
                lastInterruptedException = exception;
                break;
            }
        }

        return fallbackResponse(serviceId, attempts, lastIOException, lastInterruptedException);
    }

    private ResponseEntity<byte[]> proxiedResponse(HttpResponse<byte[]> response, String targetBase, String serviceId) {
        HttpHeaders headers = new HttpHeaders();
        response.headers().map().forEach((name, values) -> {
            if (!name.equalsIgnoreCase("transfer-encoding") && !name.equalsIgnoreCase("content-length")) {
                headers.put(name, values);
            }
        });
        headers.add("X-Gateway", "proxio-api-gateway");
        headers.add("X-Upstream-Service", serviceId);
        headers.add("X-Upstream-Base-Url", targetBase);

        return ResponseEntity.status(response.statusCode())
                .headers(headers)
                .body(response.body());
    }

    private ResponseEntity<byte[]> fallbackResponse(String serviceId,
                                                    int attempts,
                                                    IOException ioException,
                                                    InterruptedException interruptedException) {
        String reason = interruptedException != null ? interruptedException.getMessage()
                : ioException == null ? "No upstream instance available" : ioException.getMessage();
        String body = """
                {"message":"Service temporarily unavailable","service":"%s","attempts":%d,"reason":"%s"}
                """.formatted(serviceId, attempts, reason == null ? "unknown" : reason.replace("\"", "'"));
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Gateway", "proxio-api-gateway")
                .header("X-Upstream-Service", serviceId)
                .body(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String resolveServiceId(String uri) {
        if (uri.startsWith("/api/notifications")) {
            return "notification-service";
        }
        if (uri.startsWith("/api/discovery")) {
            return "discovery-service";
        }
        if (uri.startsWith("/api/config")) {
            return "config-service";
        }
        return "backend";
    }

    private void copyRequestHeaders(HttpServletRequest request, HttpRequest.Builder builder) {
        Enumeration<String> headerNames = request.getHeaderNames();
        List<String> blockedHeaders = List.of("host", "content-length", "x-internal-token");
        while (headerNames != null && headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if (blockedHeaders.contains(name.toLowerCase())) {
                continue;
            }
            Enumeration<String> values = request.getHeaders(name);
            for (String value : values == null ? Collections.<String>emptyList() : Collections.list(values)) {
                builder.header(name, value);
            }
        }

        if (request.getHeader("X-Forwarded-By") == null) {
            builder.header("X-Forwarded-By", "proxio-api-gateway");
        }
    }
}
