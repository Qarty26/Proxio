package com.proxio.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Table(name = "customers")
@Data
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String phone;

    private String address;

    @OneToMany(mappedBy = "customer")
    private List<Subscription> subscriptions;

    @OneToMany(mappedBy = "customer")
    private List<Order> orders;
}