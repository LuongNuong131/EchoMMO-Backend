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
    @Column(name = "item_id")
    private Integer itemId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String type; // WEAPON, ARMOR, CONSUMABLE...

    @Enumerated(EnumType.STRING)
    @Column(name = "slot_type")
    private SlotType slotType;

    private Integer tier;

    @Enumerated(EnumType.STRING)
    @Column(name = "rarity_default")
    private Rarity rarityDefault;

    @Column(name = "base_price")
    private BigDecimal basePrice;

    @Column(name = "image_url")
    private String imageUrl; // Lưu tên file, vd: "s_sword_0"

    @Column(name = "is_system_item")
    private Boolean isSystemItem;

    // Base Stats (Hiển thị cho shop/item gốc)
    @Column(name = "atk_bonus")
    private Integer atkBonus;

    @Column(name = "def_bonus")
    private Integer defBonus;

    @Column(name = "hp_bonus")
    private Integer hpBonus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}