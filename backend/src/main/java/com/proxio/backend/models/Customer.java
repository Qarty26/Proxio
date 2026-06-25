package com.proxio.backend.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"subscriptions", "orders", "favoriteProducts"})
public class Customer {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "User is required.")
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String phone;

    private String address;

    @OneToMany(mappedBy = "customer")
    private List<Subscription> subscriptions;

    @OneToMany(mappedBy = "customer")
    private List<Order> orders;

    @ManyToMany
    @JoinTable(
            name = "customer_favorite_products",
            joinColumns = @JoinColumn(name = "customer_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private Set<Product> favoriteProducts = new HashSet<>();
}
