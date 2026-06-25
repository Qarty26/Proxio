package com.proxio.backend.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "stocks")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"location", "product"})
public class Stock {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Location is required.")
    @ManyToOne
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @NotNull(message = "Product is required.")
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull(message = "Quantity is required.")
    @PositiveOrZero(message = "Quantity cannot be negative.")
    @Column(nullable = false)
    private Double quantity;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
