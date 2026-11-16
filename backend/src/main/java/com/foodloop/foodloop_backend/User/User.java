package com.foodloop.foodloop_backend.user;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false, unique = true) private String email;
    @Column(name = "password_hash", nullable = false) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Role role;
    @Builder.Default @Column(nullable = false) private boolean active = true;

    // profile (donor / NGO / volunteer)
    private String orgName;
    private String address;
    private Double latitude;
    private Double longitude;
    private Double capacityKg;          // NGO storage capacity
    private String acceptedFoodTypes;   // NGO: comma separated, blank = everything
    private String vehicle;             // volunteer
    @Builder.Default private boolean available = true;

    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
