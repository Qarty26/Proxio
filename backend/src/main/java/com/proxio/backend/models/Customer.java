package com.proxio.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"user", "subscriptions", "orders", "favoriteVendors"})
public class Customer {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    @JsonIgnoreProperties({"password", "vendor", "customer", "ratingsGiven", "ratingsReceived"})
    private User user;

    @Pattern(regexp = "^$|^[+0-9 .()-]{7,20}$", message = "Phone number must be valid")
    private String phone;

    @Size(max = 250, message = "Address must be at most 250 characters")
    private String address;

    @OneToMany(mappedBy = "customer")
    @JsonIgnoreProperties("customer")
    private List<Subscription> subscriptions;

    @OneToMany(mappedBy = "customer")
    @JsonIgnoreProperties("customer")
    private List<Order> orders;

    @ManyToMany
    @JoinTable(
            name = "customer_favorite_vendors",
            joinColumns = @JoinColumn(name = "customer_id"),
            inverseJoinColumns = @JoinColumn(name = "vendor_id")
    )
    @JsonIgnoreProperties({"favoritedByCustomers", "products", "locations", "subscriptions"})
    private List<Vendor> favoriteVendors;
}
