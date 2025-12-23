package com.echommo.entity;

import com.echommo.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    @JsonIgnore
    private String passwordHash;

    @Column(name = "password", nullable = false)
    @JsonIgnore
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "full_name")
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.USER;

    @Builder.Default
    @Column(name = "inventory_slots")
    private Integer inventorySlots = 49;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonIgnoreProperties("user")
    @ToString.Exclude // [FIX] Ngắt vòng lặp
    private Wallet wallet;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude // [FIX] Ngắt vòng lặp
    private Character character;

    @OneToMany(mappedBy = "seller", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude // [FIX QUAN TRỌNG] Ngắt vòng lặp vô tận với MarketListing
    private List<MarketListing> marketListings;

    @Builder.Default
    private Boolean isActive = true;
    private String banReason;
    private LocalDateTime bannedAt;

    @Builder.Default
    private Boolean isCaptchaLocked = false;

    @Builder.Default
    private Integer captchaFailCount = 0;

    private LocalDateTime captchaLockedUntil;
    private String otpCode;
    private LocalDateTime otpExpiry;

    @Column(name = "avatar_url")
    @Builder.Default
    private String avatarUrl = "🐲";

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}