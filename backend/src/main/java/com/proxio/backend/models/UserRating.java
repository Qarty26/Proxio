package com.proxio.backend.models;

import com.proxio.backend.models.enums.RatingStatus;
import jakarta.persistence.*;
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

    @ManyToOne
    @JoinColumn(name = "rater_id", nullable = false)
    private User rater;

    @ManyToOne
    @JoinColumn(name = "rated_id", nullable = false)
    private User rated;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "moderated_by")
    private User moderatedBy;

    @Column(nullable = false)
    private Integer score;

    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RatingStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime moderatedAt;
}