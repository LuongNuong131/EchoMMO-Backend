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
    private String type;
    private BigDecimal goldGained;

    // [FIX 1] Thiếu trường EXP hiện tại (Type match Integer/Long)
    // Sửa để khớp với Integer từ character.getExp()
    private Integer currentExp;

    private Integer currentLv;
    private Integer currentEnergy;
    private Integer maxEnergy;

    // [FIX 2] Thiếu trường Level mới (Tham số thứ 8)
    private Integer newLevel;
}