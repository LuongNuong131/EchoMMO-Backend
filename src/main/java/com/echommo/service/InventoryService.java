package com.echommo.service;

import com.echommo.entity.UserItem;
import com.echommo.repository.UserItemRepository;
import com.echommo.service.ItemGenerationService.SubStatDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final UserItemRepository userItemRepository;
    private final ItemGenerationService itemGenService; // Gọi thằng bên kia sang
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    private static final int MAX_LEVEL = 30;
    private static final int JUMP_INTERVAL = 3; // MỐC NHẢY: 3, 6, 9... 30 (10 lần)
    private static final int MAX_SUB_STATS = 4;

    @Transactional
    public UserItem enhanceItem(Long userItemId) {
        UserItem item = userItemRepository.findById(userItemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (item.getEnhanceLevel() >= MAX_LEVEL) {
            throw new RuntimeException("Max level reached!");
        }

        // 1. Trừ tiền/nguyên liệu (Logic này mày tự thêm sau, giờ cứ cho đập free để test)

        // 2. Tăng cấp
        int newLevel = item.getEnhanceLevel() + 1;
        item.setEnhanceLevel(newLevel);

        // 3. Tăng Main Stat (Tăng nhẹ mỗi cấp)
        increaseMainStat(item);

        // 4. CHECKPOINT: Nếu chia hết cho 3 thì NHẢY SỐ (RNG Moment)
        if (newLevel % JUMP_INTERVAL == 0) {
            applyStatJump(item);
        }

        return userItemRepository.save(item);
    }

    private void applyStatJump(UserItem item) {
        try {
            // Parse JSON cũ ra List
            List<SubStatDTO> stats = new ArrayList<>();
            if (item.getSubStats() != null && !item.getSubStats().isEmpty()) {
                stats = objectMapper.readValue(item.getSubStats(), new TypeReference<List<SubStatDTO>>() {});
            }

            // CASE A: Chưa đủ 4 dòng -> Mở dòng mới (New Substat)
            // (Giống E7: Kiếm tím +12 mới full 4 dòng, +15 nhảy vào dòng cũ)
            if (stats.size() < MAX_SUB_STATS) {
                SubStatDTO newStat = itemGenService.generateRandomSubStat(item, stats);
                stats.add(newStat);
            }
            // CASE B: Đã đủ 4 dòng -> Cộng dồn vào 1 dòng ngẫu nhiên (Enhance Substat)
            else {
                // Random chọn 1 trong 4 dòng
                int index = random.nextInt(stats.size());
                SubStatDTO target = stats.get(index);

                // Roll giá trị cộng thêm (Min-Max RNG)
                double bonus = itemGenService.getEnhanceRollValue(target.getType(), item.getItem().getTier());
                target.setValue(target.getValue() + bonus);
            }

            // Save ngược lại vào JSON
            item.setSubStats(objectMapper.writeValueAsString(stats));

        } catch (Exception e) {
            throw new RuntimeException("Lỗi RNG: " + e.getMessage());
        }
    }

    private void increaseMainStat(UserItem item) {
        // Tăng 2% giá trị gốc mỗi cấp (Ví dụ)
        if (item.getMainStatValue() != null) {
            BigDecimal current = item.getMainStatValue();
            BigDecimal increase = current.multiply(new BigDecimal("0.02"));
            item.setMainStatValue(current.add(increase));
        }
    }

    // Hàm lấy danh sách item (để controller gọi)
    public List<UserItem> getInventory(Long userId) {
        return userItemRepository.findByUser_UserId(userId);
    }
}