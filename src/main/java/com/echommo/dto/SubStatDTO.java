package com.echommo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubStatDTO {
    private String code;         // VD: "CRIT_RATE", "ATK_PERCENT"
    private double value;        // Giá trị hiện tại (Hiển thị)
    private Double originalValue; // [MYTHIC] Giá trị gốc lúc vừa lên Đỏ (để nhân %)
    private boolean isPercent;   // true = %, false = Flat
}