package com.proxio.backend.models;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Table(name = "vendors")
@Data
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String farmName;

    private String description;

    private String profileImageUrl;

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL)
    private List<Location> locations;

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL)
    private List<Product> products;

    @OneToMany(mappedBy = "vendor")
    private List<Subscription> subscriptions;
}