package com.proxio.backend.models;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "vendors")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"user", "locations", "products", "subscriptions", "favoritedByCustomers"})
public class Vendor {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    @JsonIgnoreProperties({"password", "vendor", "customer", "ratingsGiven", "ratingsReceived"})
    private User user;

    @Column(nullable = false)
    @NotBlank(message = "Farm name is required")
    @Size(min = 2, max = 120, message = "Farm name must be between 2 and 120 characters")
    private String farmName;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    private String profileImageUrl;

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("vendor")
    private List<Location> locations;

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("vendor")
    private List<Product> products;

    @OneToMany(mappedBy = "vendor")
    @JsonIgnoreProperties("vendor")
    private List<Subscription> subscriptions;

    @ManyToMany(mappedBy = "favoriteVendors")
    @JsonIgnoreProperties("favoriteVendors")
    private List<Customer> favoritedByCustomers;
}
