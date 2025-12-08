package com.echommo.service;

import com.echommo.entity.Item;
import com.echommo.entity.User;
import com.echommo.entity.UserItem;
import com.echommo.enums.Rarity;
import com.echommo.enums.SlotType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class ItemGenerationService {

    private final Random random = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // DTO nội bộ để xử lý JSON
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SubStatDTO {
        private String type;
        private Double value;
    }

    // --- 1. CORE: Hàm sinh dòng phụ mới (Dùng khi rớt đồ hoặc đập đồ mở dòng) ---
    public SubStatDTO generateRandomSubStat(UserItem userItem, List<SubStatDTO> currentStats) {
        SlotType slot = userItem.getItem().getSlotType();
        String mainStat = userItem.getMainStatType();

        while (true) {
            String candidateType = rollStatType();

            // Rule 1: Không trùng Main Stat
            if (candidateType.equals(mainStat)) continue;

            // Rule 2: Không trùng các dòng phụ đã có
            boolean isDuplicate = currentStats.stream()
                    .anyMatch(s -> s.getType().equals(candidateType));
            if (isDuplicate) continue;

            // Rule 3: Blacklist (Luật cấm của Epic 7)
            if (isBlacklisted(slot, candidateType)) continue;

            // Rule 4: Roll giá trị ban đầu (Base Roll)
            double value = getBaseRollValue(candidateType, userItem.getItem().getTier());

            return new SubStatDTO(candidateType, value);
        }
    }

    // --- 2. SUPPORT: Roll loại chỉ số ---
    private String rollStatType() {
        String[] stats = {
                "ATK_FLAT", "ATK_PERCENT",
                "DEF_FLAT", "DEF_PERCENT",
                "HP_FLAT", "HP_PERCENT",
                "SPEED",
                "CRIT_RATE", "CRIT_DMG"
        };
        return stats[random.nextInt(stats.length)];
    }

    // --- 3. SUPPORT: Check Blacklist ---
    private boolean isBlacklisted(SlotType slot, String statType) {
        switch (slot) {
            case WEAPON: // Kiếm không có Thủ
                return statType.contains("DEF");
            case ARMOR: // Áo không có Công
                return statType.contains("ATK");
            default:
                return false;
        }
    }

    // --- 4. SUPPORT: Giá trị Roll khi MỚI RA DÒNG (Tier 1) ---
    // Mày có thể chỉnh sửa Min-Max tại đây
    private double getBaseRollValue(String statType, int tier) {
        int multiplier = tier; // Tier càng cao số càng to
        switch (statType) {
            case "SPEED": return random.nextInt(3) + 2; // 2 - 4
            case "CRIT_RATE": return random.nextInt(3) + 3; // 3 - 5
            case "CRIT_DMG": return random.nextInt(4) + 4; // 4 - 7%
            case "ATK_PERCENT":
            case "HP_PERCENT":
            case "DEF_PERCENT": return random.nextInt(5) + 4; // 4 - 8%
            default: return (random.nextInt(10) + 10) * multiplier; // Flat stat
        }
    }

    // --- 5. SUPPORT: Giá trị Roll khi NHẢY DÒNG (Enhance Bonus) ---
    // Đây là cái quyết định "ngon" hay "phế" lúc đập đồ
    public double getEnhanceRollValue(String statType, int tier) {
        // Logic giống Base Roll nhưng có thể cao hơn tí nếu muốn
        return getBaseRollValue(statType, tier);
    }
}