package com.echommo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "wallet")
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Integer walletId;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    @JsonIgnore
    private User user;

    private BigDecimal gold = BigDecimal.valueOf(100);
    private Integer diamonds = 0;

    // Tài nguyên
    private Integer wood = 0;
    private Integer stone = 0;
    // private Integer fossilWood = 0; // Nếu m muốn thêm Gỗ hóa thạch

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}