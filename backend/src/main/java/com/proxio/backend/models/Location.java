package com.proxio.backend.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "Vendor is required.")
    @ManyToOne
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @NotBlank(message = "Location name is required.")
    @Column(nullable = false)
    private String name;

    private String address;

    private String city;

    private Double latitude;

    private Double longitude;

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL)
    private List<Stock> stocks;

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL)
    private List<PickupSlot> pickupSlots;

    @OneToMany(mappedBy = "location")
    private List<Order> orders;

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL)
    private List<WeeklyOffer> weeklyOffers;
}
