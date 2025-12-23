package com.echommo.config;

import java.util.Map;
import static java.util.Map.entry;

public class GameConstants {
    public static final int MAX_LEVEL = 70;
    public static final int STAT_POINTS_PER_LEVEL = 5;
    public static final double CONVERSION_RATE = 10.0;
    public static final int MAX_ASSET_VARIANTS = 5;

    // --- IDS (Giữ nguyên) ---
    public static final int MAT_WOOD_OAK = 1;
    public static final int MAT_WOOD_DRIED = 2;
    public static final int MAT_WOOD_COLD = 3;
    public static final int MAT_WOOD_BLACK = 4;
    public static final int MAT_WOOD_STRANGE = MAT_WOOD_BLACK;

    public static final int MAT_COAL = 5;
    public static final int MAT_STONE = MAT_COAL;

    public static final int MAT_ORE_COPPER = 6;
    public static final int MAT_ORE_IRON = 7;
    public static final int MAT_ORE_PLATINUM = 8;
    public static final int MAT_ORE_STRANGE = 12;
    public static final int MAT_UNKNOWN = MAT_ORE_STRANGE;

    public static final int MAT_ORE_GOLD = 14;
    public static final int MAT_FISH = 9;
    public static final int MAT_FISH_SHARK = 10;
    public static final int MAT_FISH_MEGALODON = 13;
    public static final int MAT_ECHO_COIN = 11;

    // --- MYTHIC (Giữ nguyên) ---
    public static final double[] MYTHIC_EVOLUTION_RANGE = {1.005, 1.01};
    public static final double MYTHIC_MAIN_GROWTH = 0.01;
    public static final double[] MYTHIC_SUB_GROWTH = {0.005, 0.01};

    // --- STATS WEIGHTS (Tỉ lệ xuất hiện dòng) ---
    // [FIX] Giảm tỉ lệ ra dòng ngon (Crit, Speed) để đồ xịn hiếm hơn
    public static final Map<String, Integer> STAT_WEIGHTS = Map.ofEntries(
            entry("ATK_FLAT", 100), entry("DEF_FLAT", 100), entry("HP_FLAT", 100), // Rất dễ ra
            entry("ATK_PERCENT", 50), entry("DEF_PERCENT", 50), entry("HP_PERCENT", 50), // Bình thường
            entry("ACCURACY", 30), entry("EVASION", 30), // Khá hiếm
            entry("CRIT_RATE", 20), entry("CRIT_DMG", 20), // Hiếm
            entry("SPEED", 5) // Cực hiếm (Legendary)
    );

    // --- ROLL RANGES (Biên độ chỉ số cơ bản) ---
    // [FIX CRITICAL] Giảm biên độ để phù hợp với Tier Scaling (Tier 5 sẽ x2 các số này)
    public static final Map<String, double[]> ROLL_RANGES = Map.ofEntries(
            // Chỉ số phần trăm (Tier 1: 3-5% -> Tier 5: 6-10%)
            entry("ATK_PERCENT", new double[]{3.0, 5.0}),
            entry("DEF_PERCENT", new double[]{3.0, 5.0}),
            entry("HP_PERCENT", new double[]{3.0, 5.0}),

            // Chỉ số phẳng (Tier 1: 10-20 -> Tier 5: 20-40)
            entry("ATK_FLAT", new double[]{10.0, 20.0}),
            entry("DEF_FLAT", new double[]{10.0, 20.0}),
            entry("HP_FLAT", new double[]{50.0, 100.0}), // HP cần cao hơn chút

            // Speed (Tier 1: 1-2 -> Tier 5: 2-4)
            entry("SPEED", new double[]{1.0, 2.0}),

            // Crit Rate (Tier 1: 2-4% -> Tier 5: 4-8%)
            // Nếu để 20-40% như cũ thì Tier 5 sẽ là 80% -> Quá lỗi
            entry("CRIT_RATE", new double[]{2.0, 4.0}),

            // Crit Dmg (Tier 1: 5-10% -> Tier 5: 10-20%)
            entry("CRIT_DMG", new double[]{5.0, 10.0}),

            // Accuracy / Evasion (Tier 1: 2-4%)
            entry("ACCURACY", new double[]{2.0, 4.0}),
            entry("EVASION", new double[]{2.0, 4.0})
    );
}