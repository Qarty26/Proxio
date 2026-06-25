package com.proxio.backend.models;

import com.proxio.backend.models.enums.ProductCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.Set;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"vendor", "stocks", "weeklyOffers", "favoritedByCustomers"})
public class Product {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Vendor is required.")
    @ManyToOne
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @NotBlank(message = "Product name is required.")
    @Column(nullable = false)
    private String name;

    private String description;

    private String unit;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private ProductCategory category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<Stock> stocks;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<WeeklyOffer> weeklyOffers;

    @ManyToMany(mappedBy = "favoriteProducts")
    private Set<Customer> favoritedByCustomers;
}
