package com.echommo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "wallet")
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Integer walletId;

    // [FIX] Thêm @JsonIgnore để ngắt vòng lặp vô tận
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    @JsonIgnore
    private User user;

    @Column(columnDefinition = "DECIMAL(18,2) DEFAULT 100.00")
    private BigDecimal gold;

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer diamonds;

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer wood;

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer stone;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}