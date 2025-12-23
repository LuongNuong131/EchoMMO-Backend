package com.echommo.entity;

import com.echommo.enums.Rarity;
import com.echommo.enums.SlotType;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "items")
@Data
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer itemId;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot_type")
    private SlotType slotType;

    private Integer tier;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_rarity")
    private Rarity rarity;

    @Column(name = "base_price")
    private BigDecimal basePrice;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_system_item")
    private Boolean isSystemItem;

    // [NEW] Dành cho vật phẩm giới hạn (Limited)
    @Column(name = "is_limited")
    private Boolean isLimited = false;

    @Column(name = "atk_bonus")
    private Integer atkBonus;

    @Column(name = "def_bonus")
    private Integer defBonus;

    @Column(name = "hp_bonus")
    private Integer hpBonus;

    @Column(name = "speed_bonus")
    private Integer speedBonus;

    // [NEW] CÁC CHỈ SỐ MỚI CHO TOOL
    @Column(name = "max_durability")
    private Integer maxDurability; // Độ bền tối đa (VD: 500)

    @Column(name = "min_luck")
    private Integer minLuck; // May mắn min

    @Column(name = "max_luck")
    private Integer maxLuck; // May mắn max

    @Column(name = "energy_save_chance")
    private Double energySaveChance; // Tỷ lệ tiết kiệm sức (0.1 = 10%)

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Transient
    public Integer getBaseMainStat() {
        if (atkBonus != null && atkBonus > 0) return atkBonus;
        if (defBonus != null && defBonus > 0) return defBonus;
        if (hpBonus != null && hpBonus > 0) return hpBonus;
        if (speedBonus != null && speedBonus > 0) return speedBonus;
        return 0;
    }
}