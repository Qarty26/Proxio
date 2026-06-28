package com.proxio.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"stocks", "pickupSlots", "orders", "weeklyOffers"})
public class Location {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vendor_id", nullable = false)
    @JsonIgnoreProperties({"locations", "products", "subscriptions", "favoritedByCustomers", "user"})
    private Vendor vendor;

    @Column(nullable = false)
    private String name;

    private String address;

    private String city;

    private Double latitude;

    private Double longitude;

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL)
    @JsonIgnoreProperties({"location"})
    private List<Stock> stocks;

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL)
    @JsonIgnoreProperties({"location"})
    private List<PickupSlot> pickupSlots;

    @OneToMany(mappedBy = "location")
    @JsonIgnoreProperties({"location"})
    private List<Order> orders;

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL)
    @JsonIgnoreProperties({"location"})
    private List<WeeklyOffer> weeklyOffers;
}
