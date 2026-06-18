package com.proxio.discoveryservice.model;

import jakarta.validation.constraints.NotBlank;

public record ServiceRegistration(
        @NotBlank String serviceId,
        @NotBlank String instanceId,
        @NotBlank String baseUrl
) {
}
