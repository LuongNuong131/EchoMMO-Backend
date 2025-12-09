package com.echommo.entity;

import com.echommo.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
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
    @JsonIgnoreProperties
    private String passwordHash;

    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "full_name")
    private String fullName;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    // [GIỮ NGUYÊN] Quan hệ với Wallet
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Wallet wallet;

    // [FIX MỚI] Thêm quan hệ với Character để GameService gọi được user.getCharacter()
    // Lưu ý: Logic game này đang giả định 1 User chỉ có 1 Character chính
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Character character;

    private Boolean isActive = true;
    private String banReason;
    private LocalDateTime bannedAt;

    private Boolean isCaptchaLocked = false;
    private Integer captchaFailCount = 0;
    private LocalDateTime captchaLockedUntil;

    private String otpCode;
    private LocalDateTime otpExpiry;

    private String avatarUrl = "🐲";

    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}