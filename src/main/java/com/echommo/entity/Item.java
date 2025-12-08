package com.echommo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore; // [THÊM IMPORT NÀY]
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "items")
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Integer itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore // [FIX QUAN TRỌNG] Thêm dòng này để chặn lỗi Serialization
    private User user; // Null nếu là đồ hệ thống

    private String name;
    private String description;
    private String type;
    private String rarity;

    @Column(name = "base_price")
    private BigDecimal basePrice;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_system_item")
    private Boolean isSystemItem = false;

    @Column(name = "is_equipped")
    private Boolean isEquipped = false;

    // Stats Bonus
    @Column(name = "atk_bonus")
    private Integer atkBonus = 0;

    @Column(name = "def_bonus")
    private Integer defBonus = 0;

    @Column(name = "hp_bonus")
    private Integer hpBonus = 0;

    @Column(name = "energy_bonus")
    private Integer energyBonus = 0;

    @Column(name = "speed_bonus")
    private Integer speedBonus = 0;

    @Column(name = "crit_rate_bonus")
    private Integer critRateBonus = 0;
}