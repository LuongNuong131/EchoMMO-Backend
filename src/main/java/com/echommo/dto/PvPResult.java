package com.echommo.dto;

import lombok.Data;
import java.util.List;

@Data
public class PvPResult {
    private String attackerName;
    private String defenderName;

    private Integer attackerHp;
    private Integer attackerMaxHp;

    private Integer defenderHp;
    private Integer defenderMaxHp;

    private List<String> combatLog;
    private String status; // "ONGOING", "VICTORY", "DEFEAT"

    // Phần thưởng PvP (nếu có)
    private Integer rankPointsEarned;
}