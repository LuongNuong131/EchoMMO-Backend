package com.echommo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "gift_codes")
@Data
public class GiftCode {
    @Id
    private String code;

    private String type; // "NORMAL" or "DEV_TOOL"

    @Column(name = "gold_reward")
    private Long goldReward;

    @Column(name = "coin_reward")
    private Long coinReward;

    @Column(name = "max_usages")
    private Integer maxUsages;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    @Column(name = "is_active")
    private Boolean isActive;
}