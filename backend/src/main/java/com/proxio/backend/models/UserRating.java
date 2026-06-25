package com.proxio.backend.models;

import com.proxio.backend.models.enums.RatingStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_ratings")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"rater", "rated", "order", "moderatedBy"})
public class UserRating {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Rater is required.")
    @ManyToOne
    @JoinColumn(name = "rater_id", nullable = false)
    private User rater;

    @NotNull(message = "Rated user is required.")
    @ManyToOne
    @JoinColumn(name = "rated_id", nullable = false)
    private User rated;

    @NotNull(message = "Order is required.")
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "moderated_by")
    private User moderatedBy;

    @NotNull(message = "Score is required.")
    @Min(value = 1, message = "Score must be at least 1.")
    @Max(value = 5, message = "Score must be at most 5.")
    @Column(nullable = false)
    private Integer score;

    private String comment;

    @NotNull(message = "Rating status is required.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RatingStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime moderatedAt;
}
