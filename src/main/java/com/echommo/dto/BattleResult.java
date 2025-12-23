package com.echommo.dto;

import lombok.Data;
import java.util.List;

@Data
public class BattleResult {
    // --- THÔNG TIN TRẬN ĐẤU ---
    private String enemyName;
    private Integer enemyHp;
    private Integer enemyMaxHp;
    private Integer playerHp;
    private Integer playerMaxHp;
    private List<String> combatLog;
    private String status; // "ONGOING", "VICTORY", "DEFEAT"

    // --- PHẦN THƯỞNG (Khi thắng) ---
    private Integer goldEarned;
    private Integer expEarned;

    // --- THÔNG TIN VẬT PHẨM RƠI (DROP ITEM) ---
    private boolean hasDrop;        // Có rớt đồ hay không
    private String dropName;        // Tên vật phẩm
    private String dropRarity;      // Phẩm chất (COMMON, RARE, EPIC...)
    private String dropImage;       // Đường dẫn ảnh
    private boolean inventoryFull;  // Cờ báo túi đầy (true = không nhặt được)

    // --- CÁC FIELD CŨ (Giữ lại để tương thích ngược với Frontend cũ nếu cần) ---
    private String droppedItemName;
    private String droppedItemImage;
    private String droppedItemRarity;
}