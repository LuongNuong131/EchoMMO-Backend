package com.echommo.dto;

import lombok.Data;

@Data
public class SpaStatusResponse {
    private boolean isResting;
    private String packageType;
    private Long secondsRemaining;
    private String message;
    private Integer currentHp;
    private Integer maxHp;
    private Integer currentEnergy;
    private Integer maxEnergy;
}
