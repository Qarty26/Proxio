package com.proxio.backend.models;

import com.proxio.backend.models.enums.ProductCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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

    private long vendor;

    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 500, message = "Description must be between 10 and 500 characters")
    private String description;

    @NotBlank(message = "Unit is required")
    private String unit;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private ProductCategory category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<Stock> stocks;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<WeeklyOffer> weeklyOffers;
}
