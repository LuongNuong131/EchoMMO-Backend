package com.echommo.enums;

public enum SlotType {
    NONE,
    // Trang bị chiến đấu
    WEAPON,     // Vũ khí
    ARMOR,      // Áo
    HELMET,     // Mũ
    BOOTS,      // Giày
    RING,       // Nhẫn
    NECKLACE,   // Dây chuyền

    // Công cụ khai thác (GATHERING TOOLS)
    PICKAXE,    // Cúp (Đào khoáng)
    AXE,        // Rìu (Chặt cây)
    SHOVEL,     // Xẻng (Đào đất)
    FISHING_ROD,// Cần câu (Câu cá)

    // Vật phẩm khác
    CONSUMABLE, // Vật phẩm tiêu hao
    MATERIAL    // Nguyên liệu
}