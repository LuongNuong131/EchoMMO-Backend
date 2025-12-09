package com.echommo.entity;

import com.echommo.enums.Rarity;
import com.echommo.enums.SlotType;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "items")
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer itemId;

    private String name;

    private String type; // MATERIAL, WEAPON, ARMOR, CONSUMABLE

    @Enumerated(EnumType.STRING)
    private Rarity rarity; // COMMON, RARE, EPIC...

    private Integer tier; // Cấp bậc (1, 2, 3...)

    @Column(name = "image_url")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot_type")
    private SlotType slotType; // NONE, WEAPON, HELMET, ARMOR...

    @Column(columnDefinition = "TEXT")
    private String description;

    // 👇 [FIX] Thêm giá cơ bản để MarketplaceService không báo lỗi
    @Column(name = "base_price", columnDefinition = "int default 10")
    private Integer basePrice = 10;

    // Chỉ số cơ bản (nếu là trang bị)
    private Integer attack;
    private Integer defense;
    private Integer hp;
    private Integer speed;

    public Item() {}
}