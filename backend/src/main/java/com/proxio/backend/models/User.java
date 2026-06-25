package com.proxio.backend.models;

import com.proxio.backend.models.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"vendor", "customer", "ratingsGiven", "ratingsReceived"})
public class User {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Email(message = "Please enter a valid email address.")
    @NotBlank(message = "Email is required.")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Password is required.")
    @Column(nullable = false)
    private String password;

    @NotBlank(message = "Full name is required.")
    @Column(nullable = false)
    private String fullName;

    @NotNull(message = "Role is required.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Vendor vendor;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Customer customer;

    @OneToMany(mappedBy = "rater")
    private List<UserRating> ratingsGiven;

    @OneToMany(mappedBy = "rated")
    private List<UserRating> ratingsReceived;
}
