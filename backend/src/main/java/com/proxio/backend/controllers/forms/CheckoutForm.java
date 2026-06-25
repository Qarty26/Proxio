package com.proxio.backend.controllers.forms;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutForm {

    @NotNull(message = "Quantity is required.")
    @Positive(message = "Quantity must be positive.")
    private Double quantity = 1.0;

    private Long pickupSlotId;
}
