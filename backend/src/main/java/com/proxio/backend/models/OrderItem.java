package com.proxio.backend.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"order", "weeklyOffer"})
public class OrderItem {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Order is required.")
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotNull(message = "Weekly offer is required.")
    @ManyToOne
    @JoinColumn(name = "weekly_offer_id", nullable = false)
    private WeeklyOffer weeklyOffer;

    @NotNull(message = "Quantity is required.")
    @Positive(message = "Quantity must be positive.")
    @Column(nullable = false)
    private Double quantity;

    @NotNull(message = "Price at order is required.")
    @PositiveOrZero(message = "Price cannot be negative.")
    @Column(nullable = false)
    private Double priceAtOrder;
}
