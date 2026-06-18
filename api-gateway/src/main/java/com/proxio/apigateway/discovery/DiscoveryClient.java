package com.proxio.apigateway.discovery;

import com.proxio.apigateway.config.ProxyProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DiscoveryClient {

    private static final Pattern INSTANCE_PATTERN = Pattern.compile(
            "\\{[^{}]*\"serviceId\"\\s*:\\s*\"([^\"]+)\"[^{}]*\"instanceId\"\\s*:\\s*\"([^\"]+)\"[^{}]*\"baseUrl\"\\s*:\\s*\"([^\"]+)\"[^{}]*}"
    );

    private final HttpClient httpClient;
    private final ProxyProperties proxyProperties;

    public DiscoveryClient(HttpClient httpClient, ProxyProperties proxyProperties) {
        this.httpClient = httpClient;
        this.proxyProperties = proxyProperties;
    }

    public List<ServiceInstance> instances(String serviceId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(proxyProperties.discovery() + "/api/discovery/services/" + serviceId))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                return List.of();
            }
            return parseInstances(response.body());
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<ServiceInstance> parseInstances(String json) {
        Matcher matcher = INSTANCE_PATTERN.matcher(json);
        java.util.ArrayList<ServiceInstance> instances = new java.util.ArrayList<>();
        while (matcher.find()) {
            instances.add(new ServiceInstance(matcher.group(1), matcher.group(2), matcher.group(3)));
        }
        return instances;
    }
}
