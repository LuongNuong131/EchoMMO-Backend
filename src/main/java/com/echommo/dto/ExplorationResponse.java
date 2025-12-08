package com.echommo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExplorationResponse {
    private String message;
    private String type;       // NOTHING, GOLD, ITEM, COMBAT, REST...
    private BigDecimal goldGained; // Tổng vàng nhận được (Base + Event)

    private Integer currentExp;
    private Integer currentLv;
    private Integer currentEnergy;
    private Integer maxEnergy;
    private Integer newLevel;

    // [NEW] Thêm 2 trường này để Frontend hiển thị popup nhận quà
    private String rewardName;   // Ví dụ: "Túi vàng", "Gỗ", "Kiếm cùn"
    private Integer rewardAmount; // Ví dụ: 50, 1, 1
}