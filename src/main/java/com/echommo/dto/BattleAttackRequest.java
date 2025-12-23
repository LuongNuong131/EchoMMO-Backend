package com.echommo.dto;

import lombok.Data;
import java.util.Map;

@Data
public class BattleAttackRequest {
    private Long enemyId;
    private Integer enemyHp;
    private Boolean isBuffed;

    // Map chứa chỉ số gửi từ client (nếu có)
    private Map<String, Integer> playerStats;
}