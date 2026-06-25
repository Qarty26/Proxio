package com.proxio.backend.controllers.forms;

import com.proxio.backend.models.enums.ProductCategory;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class VendorProductOfferForm {

    @NotBlank(message = "Product name is required.")
    private String name;

    private String description;

    @NotBlank(message = "Unit is required.")
    private String unit = "kg";

    @NotNull(message = "Category is required.")
    private ProductCategory category = ProductCategory.VEGETABLES;

    @NotNull(message = "Stock quantity is required.")
    @Positive(message = "Stock quantity must be positive.")
    private Double stockQuantity = 10.0;

    @NotNull(message = "Offer quantity is required.")
    @Positive(message = "Offer quantity must be positive.")
    private Double offerQuantity = 10.0;

    @NotNull(message = "Price is required.")
    @Positive(message = "Price must be positive.")
    private Double pricePerUnit = 10.0;

    @FutureOrPresent(message = "Valid until must be today or later.")
    @NotNull(message = "Valid until is required.")
    private LocalDate validUntil = LocalDate.now().plusDays(7);

    private String note;

    @NotBlank(message = "Pickup location name is required.")
    private String locationName = "Farm pickup";

    private String city = "Brasov";
}
