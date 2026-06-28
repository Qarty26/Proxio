package com.proxio.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.proxio.backend.models.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"customer", "location", "pickupSlot", "items", "ratings"})
public class Order {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnoreProperties({"orders", "subscriptions", "favoriteVendors"})
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "location_id", nullable = false)
    @JsonIgnoreProperties({"orders", "stocks", "pickupSlots", "weeklyOffers", "vendor"})
    private Location location;

    @ManyToOne
    @JoinColumn(name = "pickup_slot_id")
    @JsonIgnoreProperties({"location"})
    private PickupSlot pickupSlot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @JsonIgnoreProperties({"order"})
    private List<OrderItem> items;

    @OneToMany(mappedBy = "order")
    @JsonIgnoreProperties({"order"})
    private List<UserRating> ratings;
}
