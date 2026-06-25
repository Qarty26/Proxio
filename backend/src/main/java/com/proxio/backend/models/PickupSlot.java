package com.proxio.backend.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pickup_slots")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"location"})
public class PickupSlot {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Location is required.")
    @ManyToOne
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @NotNull(message = "Start time is required.")
    @Column(nullable = false)
    private LocalDateTime startTime;

    @NotNull(message = "End time is required.")
    @Column(nullable = false)
    private LocalDateTime endTime;

    @Positive(message = "Maximum orders must be positive.")
    private Integer maxOrders;
}
