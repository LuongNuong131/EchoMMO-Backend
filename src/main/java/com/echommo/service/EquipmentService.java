package com.echommo.service;

import com.echommo.config.GameConstants;
import com.echommo.dto.SubStatDTO;
import com.echommo.entity.UserItem;
import com.echommo.entity.Wallet;
import com.echommo.enums.Rarity;
import com.echommo.repository.UserItemRepository;
import com.echommo.repository.WalletRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class EquipmentService {

    // Dependencies
    @Autowired private UserItemRepository userItemRepo;
    @Autowired private WalletRepository walletRepo;
    @Autowired private ItemGenerationService itemGenService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    // ==========================================================
    // UTILS (COST & JSON)
    // ==========================================================

    private String getRequiredMaterial(int currentEnhanceLevel) {
        if (currentEnhanceLevel < 5) return "Gỗ"; // Level 0-4
        if (currentEnhanceLevel < 10) return "Đá"; // Level 5-9
        if (currentEnhanceLevel < 20) return "Sắt"; // Level 10-19
        return "Bạch Kim"; // Level 20-29
    }

    private int getRequiredQuantity(int currentEnhanceLevel) {
        return (currentEnhanceLevel / 5) + 1;
    }

    private double getBaseBoostFactor(int level) {
        return 1.0 + level * 0.05;
    }

    private List<SubStatDTO> parseSubStats(String json) {
        try {
            if (json == null || json.isEmpty() || json.equals("[]")) return new ArrayList<>();
            return objectMapper.readValue(json, new TypeReference<List<SubStatDTO>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // ==========================================================
    // 1. CƯỜNG HÓA THƯỜNG (+1 -> +30)
    // ==========================================================
    @Transactional
    public UserItem upgradeItem(Long userItemId, Integer userId) throws Exception {
        UserItem item = userItemRepo.findById(userItemId).orElseThrow(() -> new EntityNotFoundException("Item không tồn tại"));

        if (!item.getUser().getUserId().equals(userId)) throw new RuntimeException("Item không chính chủ");
        if (item.getEnhanceLevel() >= 30) throw new RuntimeException("Đã đạt cấp tối đa (+30)");

        // 1. Kiểm tra và Trừ chi phí VÀNG
        Wallet wallet = walletRepo.findByUser_UserId(userId)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy ví người chơi."));

        BigDecimal GOLD_COST = BigDecimal.valueOf(50 + item.getEnhanceLevel() * 10);

        if (wallet.getGold().compareTo(GOLD_COST) < 0) {
            throw new RuntimeException("Không đủ Vàng! (Cần: " + GOLD_COST.intValue() + ")");
        }

        wallet.setGold(wallet.getGold().subtract(GOLD_COST));


        // 2. Kiểm tra và Trừ nguyên liệu
        String matName = getRequiredMaterial(item.getEnhanceLevel());
        int qtyRequired = getRequiredQuantity(item.getEnhanceLevel());

        // --- CHECK FOR BASIC RESOURCES IN WALLET (Gỗ / Đá) ---
        int currentQty = 0;

        if (matName.equals("Gỗ")) {
            currentQty = (wallet.getWood() != null ? wallet.getWood() : 0);

            if (currentQty < qtyRequired) {
                throw new RuntimeException("Không đủ Gỗ! (Cần: " + qtyRequired + ")");
            }

            wallet.setWood(currentQty - qtyRequired);

        } else if (matName.equals("Đá")) {
            currentQty = (wallet.getStone() != null ? wallet.getStone() : 0);

            if (currentQty < qtyRequired) {
                throw new RuntimeException("Không đủ Đá! (Cần: " + qtyRequired + ")");
            }

            wallet.setStone(currentQty - qtyRequired);

        }
        // --- CHECK FOR OTHER MATERIALS AS USERITEM (Sắt, Bạch Kim) ---
        else {
            UserItem materialInBag = userItemRepo.findByUser_UserIdAndItem_Name(userId, matName)
                    .orElseThrow(() -> new RuntimeException("Thiếu nguyên liệu! Cần " + qtyRequired + " " + matName));

            if (materialInBag.getQuantity() < qtyRequired) {
                throw new RuntimeException("Không đủ " + matName + "! (Cần: " + qtyRequired + ")");
            }

            // Trừ vật liệu và xóa nếu hết
            materialInBag.setQuantity(materialInBag.getQuantity() - qtyRequired);
            if (materialInBag.getQuantity() <= 0) {
                userItemRepo.delete(materialInBag);
            } else {
                userItemRepo.save(materialInBag);
            }
        }

        // --- Save Wallet (Lưu lại việc trừ Vàng và Gỗ/Đá) ---
        walletRepo.save(wallet);


        // 3. Tăng cấp (+1)
        int newLevel = item.getEnhanceLevel() + 1;
        item.setEnhanceLevel(newLevel);

        // 4. Tăng Main Stat (Xử lý Null Main Stat Value)
        BigDecimal mainStat = item.getMainStatValue();
        if (mainStat == null) {
            // Gán giá trị 1.00 làm nền để tránh crash
            mainStat = BigDecimal.valueOf(1.00).setScale(2, RoundingMode.HALF_UP);
            item.setMainStatValue(mainStat);
        }

        BigDecimal oldDenominator = BigDecimal.valueOf(getBaseBoostFactor(newLevel - 1));
        BigDecimal base = mainStat.divide(oldDenominator, 2, RoundingMode.HALF_UP);
        BigDecimal newNumerator = BigDecimal.valueOf(getBaseBoostFactor(newLevel));
        item.setMainStatValue(base.multiply(newNumerator).setScale(2, RoundingMode.HALF_UP));

        // 5. CHECK MỐC +3, +6, +9... ĐỂ ROLL SUBSTAT
        if (newLevel % 3 == 0) {
            handleSubStatRoll(item);
        }

        return userItemRepo.save(item);
    }

    private void handleSubStatRoll(UserItem item) throws Exception {
        // [CRITICAL FIX: Xử lý Null Rarity]
        Rarity rarity = item.getRarity();
        if (rarity == null) {
            rarity = Rarity.COMMON; // Gán mặc định COMMON để tránh crash
            item.setRarity(rarity);
            // KHÔNG CẦN userItemRepo.save(item) ở đây, vì nó sẽ được lưu cuối hàm upgradeItem
        }

        List<SubStatDTO> stats = parseSubStats(item.getSubStats());
        // Sử dụng biến rarity đã được kiểm tra
        int maxSlots = rarity.maxSubStats;

        if (stats.size() < maxSlots) {
            // A. Mở dòng mới (Nếu chưa full dòng)
            stats.add(itemGenService.generateRandomSubStat(item, stats));
        } else {
            // B. Đã full dòng -> Random chọn 1 dòng để cộng thêm
            SubStatDTO target = stats.get(random.nextInt(stats.size()));

            // Roll giá trị cộng thêm (Dùng logic từ ItemGenerationService)
            double bonus = itemGenService.getEnhanceRollValue(target.getCode(), item.getItem().getTier());
            target.setValue(round(target.getValue() + bonus));
        }

        // Lưu lại JSON
        item.setSubStats(objectMapper.writeValueAsString(stats));
    }


    // ==========================================================
    // 2. TIẾN HÓA MYTHIC (TỶ LỆ 5% - GIỮ STAT GỐC)
    // ==========================================================
    @Transactional
    public String evolveToMythic(Long userItemId, Integer userId) throws Exception {
        UserItem item = userItemRepo.findById(userItemId).orElseThrow();

        if (!item.getUser().getUserId().equals(userId)) throw new RuntimeException("Item không chính chủ");
        if (item.getRarity() != Rarity.LEGENDARY || item.getEnhanceLevel() < 30) return "Chưa đủ điều kiện tiến hóa.";

        // 1. Logic Trừ Nguyên liệu Mythic (Kim cương, token hiếm...)

        // 2. LOGIC 5%
        if (random.nextInt(100) >= 5) { // 95% thất bại
            return "Tiến hóa thất bại! Bạn mất nguyên liệu.";
        }

        // 3. THÀNH CÔNG & SNAPSHOT
        item.setRarity(Rarity.MYTHIC);
        item.setMythic(true);
        item.setMythicLevel(1);

        // A. Snapshot Main Stat & Tăng 1%
        item.setOriginalMainStatValue(item.getMainStatValue());
        BigDecimal newMain = item.getOriginalMainStatValue().multiply(new BigDecimal("1.01"));
        item.setMainStatValue(newMain.setScale(2, RoundingMode.HALF_UP));

        // B. Snapshot Sub Stats & Tăng 1%
        List<SubStatDTO> stats = parseSubStats(item.getSubStats());
        for (SubStatDTO s : stats) {
            if (s.getOriginalValue() != null) {
                s.setValue(round(s.getOriginalValue() * 1.01));
            }
        }

        item.setSubStats(objectMapper.writeValueAsString(stats));
        userItemRepo.save(item);

        return "CHÚC MỪNG! Trang bị đã đạt cấp Thần Thoại!";
    }

    // ==========================================================
    // 3. NÂNG CẤP MYTHIC (NHÂN % THEO CẤP)
    // ==========================================================
    @Transactional
    public UserItem upgradeMythicLevel(Long userItemId) throws Exception {
        UserItem item = userItemRepo.findById(userItemId).orElseThrow();

        if (!item.isMythic()) throw new RuntimeException("Không phải đồ Mythic");
        if (item.getMythicLevel() >= 30) throw new RuntimeException("Đã đạt cấp Mythic tối đa!");

        // 1. Logic trừ chi phí Mythic (Vàng/Nguyên liệu hiếm)

        int nextLv = item.getMythicLevel() + 1;
        item.setMythicLevel(nextLv);

        // Công thức: Stat = Gốc * (1 + Level * 0.01)
        double multiplier = 1 + (nextLv * 0.01);

        // 2. Update Main Stat
        BigDecimal multiBD = BigDecimal.valueOf(multiplier);
        item.setMainStatValue(item.getOriginalMainStatValue().multiply(multiBD).setScale(2, RoundingMode.HALF_UP));

        // 3. Update Sub Stats
        List<SubStatDTO> stats = parseSubStats(item.getSubStats());
        for (SubStatDTO s : stats) {
            if (s.getOriginalValue() != null) {
                s.setValue(round(s.getOriginalValue() * multiplier));
            }
        }

        item.setSubStats(objectMapper.writeValueAsString(stats));
        return userItemRepo.save(item);
    }
}