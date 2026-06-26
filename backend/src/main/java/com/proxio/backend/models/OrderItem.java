package com.proxio.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"order", "weeklyOffer", "product"})
public class OrderItem {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnoreProperties({"items", "ratings", "customer", "pickupSlot"})
    private Order order;

    // Nullable — used for WeeklyOffer-based ordering
    @ManyToOne
    @JoinColumn(name = "weekly_offer_id")
    @JsonIgnoreProperties({"orderItems"})
    private WeeklyOffer weeklyOffer;

    // Used for direct product ordering (weeklyOffer null in this case)
    @ManyToOne
    @JoinColumn(name = "product_id")
    @JsonIgnoreProperties({"stocks", "weeklyOffers", "vendor"})
    private Product product;

    @Column(nullable = false)
    private Double quantity;

    @Column(nullable = false)
    private Double priceAtOrder;
}
