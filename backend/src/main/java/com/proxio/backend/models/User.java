package com.proxio.backend.models;

import com.proxio.backend.models.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @Column(nullable = false, unique = true)
    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    private String email;

    @Column(nullable = false)
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must contain at least 6 characters")
    private String password;

    @Column(nullable = false)
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 120, message = "Full name must be between 2 and 120 characters")
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Role is required")
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
