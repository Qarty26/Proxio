package com.proxio.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.proxio.backend.models.enums.ProductCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"vendor", "stocks", "weeklyOffers"})
public class Product {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vendor_id", nullable = false)
    @NotNull(message = "Vendor is required")
    @JsonIgnoreProperties({"user", "locations", "products", "subscriptions", "favoritedByCustomers"})
    private Vendor vendor;

    @Column(nullable = false)
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 120, message = "Name must be between 2 and 120 characters")
    private String name;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 500, message = "Description must be between 10 and 500 characters")
    private String description;

    @NotBlank(message = "Unit is required")
    @Size(max = 40, message = "Unit must be at most 40 characters")
    private String unit;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Category is required")
    private ProductCategory category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<Stock> stocks;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<WeeklyOffer> weeklyOffers;
}
