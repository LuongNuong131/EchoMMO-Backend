package com.echommo.dto;
import lombok.Data;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ExplorationResponse {
    private String message;       // Ví dụ: "Bạn tìm thấy 10 vàng!"
    private String rewardType;    // GOLD, EXP, ITEM, NOTHING
    private BigDecimal goldGained;
    private Long expGained;
    private Integer currentEnergy;
    private Integer maxEnergy;
    private Integer newLevel;     // Nếu lên cấp thì trả về level mới
}