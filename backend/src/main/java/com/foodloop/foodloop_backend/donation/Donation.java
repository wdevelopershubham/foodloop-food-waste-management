package com.foodloop.foodloop_backend.donation;

import com.foodloop.foodloop_backend.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "donations")
@Getter @Setter @NoArgsConstructor
public class Donation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private User donor;
    @ManyToOne private User ngo;
    @ManyToOne private User volunteer;

    @Column(nullable = false) private String foodCategory;
    @Column(nullable = false) private double quantityKg;
    private boolean vegetarian = true;
    private String description;
    private LocalDateTime preparedAt;
    @Column(nullable = false) private LocalDateTime expiresAt;
    private String pickupAddress;
    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING) @Column(nullable = false) private DonationStatus status = DonationStatus.CREATED;

    @Column(nullable = false, unique = true) private String pickupToken;
    @Column(nullable = false, unique = true) private String deliveryToken;

    private LocalDateTime createdAt, acceptedAt, assignedAt, pickedUpAt, deliveredAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
