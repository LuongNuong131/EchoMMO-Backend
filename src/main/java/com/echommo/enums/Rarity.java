package com.echommo.enums;

public enum Rarity {
    COMMON(1),
    UNCOMMON(2),  // Thêm cái này (Tương ứng xanh lá)
    RARE(3),      // Xanh dương
    EPIC(4),      // Tím
    LEGENDARY(4), // Vàng
    MYTHIC(4);    // Thêm cái này (Đỏ)

    public final int maxSubStats;

    Rarity(int maxSubStats) {
        this.maxSubStats = maxSubStats;
    }
}