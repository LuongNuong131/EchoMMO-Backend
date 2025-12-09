package com.echommo.service;

// [QUAN TRỌNG] Import DTO chuẩn từ package dto
import com.echommo.dto.SubStatDTO;
import com.echommo.entity.UserItem;
import com.echommo.enums.SlotType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class ItemGenerationService {

    private final Random random = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // [QUAN TRỌNG] ĐÃ XÓA class SubStatDTO nội bộ để dùng chung DTO với BattleService

    // --- 1. CORE: Hàm sinh dòng phụ mới ---
    public SubStatDTO generateRandomSubStat(UserItem userItem, List<SubStatDTO> currentStats) {
        SlotType slot = userItem.getItem().getSlotType();
        String mainStat = userItem.getMainStatType();

        while (true) {
            String candidateType = rollStatType();

            // Rule 1: Không trùng Main Stat
            if (candidateType.equals(mainStat)) continue;

            // Rule 2: Không trùng các dòng phụ đã có
            // [FIX] DTO chuẩn dùng .getCode() chứ không phải .getType()
            boolean isDuplicate = currentStats.stream()
                    .anyMatch(s -> s.getCode().equals(candidateType));
            if (isDuplicate) continue;

            // Rule 3: Blacklist (Luật cấm của Epic 7)
            if (isBlacklisted(slot, candidateType)) continue;

            // Rule 4: Roll giá trị ban đầu (Base Roll)
            double value = getBaseRollValue(candidateType, userItem.getItem().getTier());

            // [FIX] Xác định isPercent để Frontend hiển thị đúng
            boolean isPercent = candidateType.contains("PERCENT")
                    || candidateType.equals("CRIT_RATE")
                    || candidateType.equals("CRIT_DMG");

            // [FIX] Trả về DTO chuẩn (code, value, originalValue, isPercent)
            // originalValue để null vì mới tạo chưa lên Mythic
            return new SubStatDTO(candidateType, value, null, isPercent);
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
    private double getBaseRollValue(String statType, int tier) {
        int multiplier = Math.max(1, tier); // Đảm bảo tier ít nhất là 1

        switch (statType) {
            case "SPEED":
                // Speed rất hiếm, tier cao mới tăng nhẹ
                return random.nextInt(3) + 1 + (tier > 3 ? 1 : 0);
            case "CRIT_RATE":
                return random.nextInt(3) + 2; // 2 - 4%
            case "CRIT_DMG":
                return random.nextInt(4) + 4; // 4 - 7%
            case "ATK_PERCENT":
            case "HP_PERCENT":
            case "DEF_PERCENT":
                return random.nextInt(5) + 4; // 4 - 8%
            default:
                // Flat stat nhân mạnh theo Tier
                return (random.nextInt(10) + 10) * multiplier;
        }
    }

    // --- 5. SUPPORT: Giá trị Roll khi NHẢY DÒNG (Enhance Bonus) ---
    public double getEnhanceRollValue(String statType, int tier) {
        return getBaseRollValue(statType, tier);
    }
}