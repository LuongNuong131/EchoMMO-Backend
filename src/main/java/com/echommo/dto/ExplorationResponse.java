// EchoMMO-Backend/src/main/java/com/echommo/dto/ExplorationResponse.java
package com.echommo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExplorationResponse {
    // --- LEGACY ---
    private String message;
    private String type;
    private BigDecimal goldGained;
    private Long currentExp;
    private Integer currentLv;
    private Integer currentEnergy;
    private Integer maxEnergy;
    private Integer newLevel;

    private String rewardName;      // Tên hiển thị (Gỗ Sồi)
    private String rewardItemCode;  // [NEW] Mã code để lấy ảnh (w_wood)
    private Integer rewardAmount;
    private Integer rewardItemId;

    private String itemReceived;
    private int quantity;

    // --- NEW ---
    private String log;
    private String rewardType;
    private String rewardValue;
    private String imageUrl;
    private boolean specialReward;
}