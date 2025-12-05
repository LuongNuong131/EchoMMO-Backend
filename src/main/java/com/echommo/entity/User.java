package com.echommo.entity;

import com.echommo.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    @JsonIgnore
    private String passwordHash;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "full_name")
    private String fullName;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // --- BAN INFO (MỚI) ---
    @Column(name = "ban_reason")
    private String banReason;

    @Column(name = "banned_at")
    private LocalDateTime bannedAt;
    // ----------------------

    // --- CAPTCHA FIELDS ---
    @Column(name = "is_captcha_locked")
    private Boolean isCaptchaLocked = false;

    @Column(name = "captcha_fail_count")
    private Integer captchaFailCount = 0;

    @Column(name = "captcha_locked_until")
    private LocalDateTime captchaLockedUntil;

    // --- OTP FIELDS ---
    @Column(name = "otp_code")
    private String otpCode;

    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;

    @Column(name = "avatar_url")
    private String avatarUrl = "🐲"; // Icon mặc định

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Wallet wallet;
}