package com.echommo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaStatusResponse {
    private String message;
    private Integer currentHp;
    private Integer currentEnergy;
    private BigDecimal currentGold; // [FIX] Đồng bộ BigDecimal
    private BigDecimal currentEcho; // [FIX] Đồng bộ BigDecimal
}