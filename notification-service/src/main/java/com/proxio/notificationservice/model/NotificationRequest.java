package com.proxio.notificationservice.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NotificationRequest(
        @NotBlank(message = "Recipient is required")
        @Email(message = "Recipient must be a valid email")
        String recipient,

        @NotBlank(message = "Channel is required")
        @Pattern(regexp = "email|sms|push", message = "Channel must be email, sms, or push")
        String channel,

        @NotBlank(message = "Message is required")
        @Size(max = 280, message = "Message must be at most 280 characters")
        String message
) {
}
