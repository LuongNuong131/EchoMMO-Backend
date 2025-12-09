package com.echommo.config;

import java.util.Map;

public class GameConstants {

    // 1. Cấu hình Range Random khi nâng cấp (Min, Max)
    // Ví dụ: ATK% tăng từ 4% đến 8% mỗi lần roll
    public static final Map<String, double[]> ROLL_RANGES = Map.of(
            "ATK_PERCENT", new double[]{4.0, 8.0},
            "HP_PERCENT", new double[]{4.0, 8.0},
            "DEF_PERCENT", new double[]{4.0, 8.0},
            "CRIT_RATE", new double[]{2.0, 5.0},
            "CRIT_DMG", new double[]{3.0, 7.0},
            "SPEED", new double[]{1.0, 4.0}, // Speed tăng ít
            "ATK_FLAT", new double[]{20.0, 50.0}
    );

    // 2. Trọng số xuất hiện (Weight) - Tổng không cần bằng 100
    // Speed để thấp (2) thì cực khó ra so với ATK (20)
    public static final Map<String, Integer> STAT_WEIGHTS = Map.of(
            "ATK_PERCENT", 20,
            "HP_PERCENT", 20,
            "DEF_PERCENT", 20,
            "CRIT_RATE", 12,
            "CRIT_DMG", 12,
            "SPEED", 2,
            "ATK_FLAT", 15
    );
}