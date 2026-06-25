package com.proxio.backend.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "weekly_offers")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"product", "location", "orderItems"})
public class WeeklyOffer {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Product is required.")
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull(message = "Location is required.")
    @ManyToOne
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @NotNull(message = "Price is required.")
    @PositiveOrZero(message = "Price cannot be negative.")
    @Column(nullable = false)
    private Double pricePerUnit;

    @NotNull(message = "Available quantity is required.")
    @PositiveOrZero(message = "Available quantity cannot be negative.")
    @Column(nullable = false)
    private Double availableQuantity;

    @NotNull(message = "Valid from date is required.")
    @Column(nullable = false)
    private LocalDate validFrom;

    @NotNull(message = "Valid until date is required.")
    @Column(nullable = false)
    private LocalDate validUntil;

    private String note;

    @OneToMany(mappedBy = "weeklyOffer")
    private List<OrderItem> orderItems;
}
