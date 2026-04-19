package com.proxio.backend.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "order_items")
@Data
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "weekly_offer_id", nullable = false)
    private WeeklyOffer weeklyOffer;

    @Column(nullable = false)
    private Double quantity;

    @Column(nullable = false)
    private Double priceAtOrder;
}