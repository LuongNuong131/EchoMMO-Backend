package com.echommo.service;

import com.echommo.config.GameConstants;
import com.echommo.dto.SubStatDTO;
import com.echommo.entity.Item;
import com.echommo.entity.UserItem;
import com.echommo.enums.SlotType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class ItemGenerationService {

    private final Random random = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Khởi tạo các thông số ngẫu nhiên cho một trang bị mới nhận được.
     * [FIX]: Chỉ số chính (Main Stat) giờ đây tỷ lệ thuận với Tier.
     */
    public void randomizeNewItem(UserItem userItem) {
        Item baseItem = userItem.getItem();
        int tier = (baseItem.getTier() != null) ? baseItem.getTier() : 1;

        // Gán biến thể hình ảnh ngẫu nhiên
        userItem.setVisualVariant(random.nextInt(GameConstants.MAX_ASSET_VARIANTS));

        // 1. Tính toán lại Main Stat dựa trên Tier
        if (userItem.getMainStatValue() != null && userItem.getMainStatValue().compareTo(BigDecimal.ZERO) > 0) {
            // Công thức: Giá trị gốc * (1 + Tier * 0.5) * Biên độ ngẫu nhiên (0.9 - 1.1)
            double tierMultiplier = 1.0 + (tier * 0.5);
            double variance = 0.9 + (random.nextDouble() * 0.2);

            BigDecimal newVal = userItem.getMainStatValue()
                    .multiply(BigDecimal.valueOf(tierMultiplier))
                    .multiply(BigDecimal.valueOf(variance));

            userItem.setMainStatValue(newVal);
            userItem.setOriginalMainStatValue(newVal);
        }

        // 2. Xác định số lượng chỉ số phụ (Sub-stats) dựa trên độ hiếm
        int initialSubs = switch (baseItem.getRarity()) {
            case UNCOMMON -> 1;
            case RARE -> 2;
            case EPIC -> 3;
            case LEGENDARY, MYTHIC -> 4;
            default -> 0;
        };

        // 3. Tạo danh sách chỉ số phụ
        List<SubStatDTO> stats = new ArrayList<>();
        for (int i = 0; i < initialSubs; i++) {
            stats.add(generateRandomSubStat(userItem, stats));
        }

        try {
            userItem.setSubStats(objectMapper.writeValueAsString(stats));
        } catch (Exception e) {
            userItem.setSubStats("[]");
        }
    }

    /**
     * Tạo một chỉ số phụ ngẫu nhiên không trùng lặp và không nằm trong danh sách đen của Slot đồ.
     */
    public SubStatDTO generateRandomSubStat(UserItem userItem, List<SubStatDTO> currentStats) {
        SlotType slot = userItem.getItem().getSlotType();
        int tier = (userItem.getItem().getTier() != null) ? userItem.getItem().getTier() : 1;

        while (true) {
            String candidateType = rollStatTypeWithWeight();

            // Kiểm tra trùng lặp với các dòng hiện có
            boolean isDuplicate = currentStats.stream().anyMatch(s -> s.getCode().equals(candidateType));
            if (isDuplicate || isBlacklisted(slot, candidateType)) continue;

            double value = rollValue(candidateType, tier);
            boolean isPercent = candidateType.contains("PERCENT") ||
                    List.of("CRIT_RATE", "CRIT_DMG", "ACCURACY", "EVASION").contains(candidateType);

            return new SubStatDTO(candidateType, value, null, isPercent);
        }
    }

    /**
     * Trả về giá trị cộng thêm khi cường hóa (Enhance) một chỉ số.
     */
    public double getEnhanceRollValue(String statCode, int tier) {
        // Giá trị cộng thêm dựa trên 1 lần roll chuẩn theo Tier
        return rollValue(statCode, tier);
    }

    /**
     * Lấy ngẫu nhiên loại chỉ số dựa trên trọng số (Weight) trong GameConstants.
     */
    private String rollStatTypeWithWeight() {
        int totalWeight = GameConstants.STAT_WEIGHTS.values().stream().mapToInt(Integer::intValue).sum();
        int roll = random.nextInt(totalWeight);
        int currentSum = 0;

        for (Map.Entry<String, Integer> entry : GameConstants.STAT_WEIGHTS.entrySet()) {
            currentSum += entry.getValue();
            if (roll < currentSum) return entry.getKey();
        }
        return "HP_FLAT";
    }

    /**
     * Ngăn chặn các chỉ số vô lý trên trang bị (VD: Giáp không thể có Xuyên giáp/Chính xác cao).
     */
    private boolean isBlacklisted(SlotType slot, String statType) {
        if (slot == SlotType.WEAPON) return statType.contains("DEF") || statType.equals("EVASION");
        if (slot == SlotType.ARMOR) return statType.contains("ATK") || statType.equals("ACCURACY");
        return false;
    }

    /**
     * Tính toán giá trị của chỉ số.
     * [FIX]: Tier ảnh hưởng trực tiếp đến sức mạnh của chỉ số phụ.
     */
    private double rollValue(String statType, int tier) {
        double[] range = GameConstants.ROLL_RANGES.getOrDefault(statType, new double[]{1.0, 2.0});

        // Công thức: (Min + (Max - Min) * ngẫu nhiên) * hệ số Tier
        double baseRoll = range[0] + (range[1] - range[0]) * random.nextDouble();
        double finalValue = baseRoll * (1 + (tier - 1) * 0.25); // Mỗi Tier tăng thêm 25% sức mạnh dòng phụ

        return Math.round(finalValue * 100.0) / 100.0;
    }
}