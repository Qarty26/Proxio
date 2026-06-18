package com.proxio.notificationservice.discovery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class ServiceRegistrationClient implements ApplicationRunner {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private final String discoveryBaseUrl;
    private final String serviceId;
    private final String instanceId;
    private final String serviceBaseUrl;

    public ServiceRegistrationClient(@Value("${discovery.base-url:http://localhost:8761}") String discoveryBaseUrl,
                                     @Value("${discovery.service-id:${spring.application.name}}") String serviceId,
                                     @Value("${discovery.instance-id:${HOSTNAME:${spring.application.name}-local}}") String instanceId,
                                     @Value("${discovery.service-base-url:http://localhost:8082}") String serviceBaseUrl) {
        this.discoveryBaseUrl = discoveryBaseUrl;
        this.serviceId = serviceId;
        this.instanceId = instanceId;
        this.serviceBaseUrl = serviceBaseUrl;
    }

    @Override
    public void run(ApplicationArguments args) {
        register();
    }

    @Scheduled(fixedDelayString = "${discovery.heartbeat-ms:30000}")
    public void heartbeat() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(discoveryBaseUrl + "/api/discovery/services/" + serviceId + "/" + instanceId + "/heartbeat"))
                    .timeout(Duration.ofSeconds(2))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() == 404) {
                register();
            }
        } catch (Exception ignored) {
            // The service remains usable when discovery is temporarily unavailable.
        }
    }

    private void register() {
        try {
            String body = registrationJson();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(discoveryBaseUrl + "/api/discovery/register"))
                    .timeout(Duration.ofSeconds(2))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
            // Startup should not fail just because the registry is not ready yet.
        }
    }

    private String registrationJson() {
        return """
                {"serviceId":"%s","instanceId":"%s","baseUrl":"%s"}
                """.formatted(jsonEscape(serviceId), jsonEscape(instanceId), jsonEscape(serviceBaseUrl));
    }

    private String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
