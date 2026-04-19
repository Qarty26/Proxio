package com.proxio.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Table(name = "locations")
@Data
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

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