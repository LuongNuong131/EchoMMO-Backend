package com.echommo.service;

import com.echommo.entity.*;
import com.echommo.entity.Character;
import com.echommo.enums.Rarity;
import com.echommo.repository.*;
import com.echommo.service.ItemGenerationService.SubStatDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional
public class GameService {

    @Autowired private UserRepository userRepo;
    @Autowired private CharacterRepository charRepo;
    @Autowired private WalletRepository walletRepo;
    @Autowired private UserItemRepository userItemRepo;
    @Autowired private ItemRepository itemRepo;
    @Autowired private ItemGenerationService itemGenService;
    @Autowired private CharacterService characterService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();
    private static final BigDecimal REST_COST = new BigDecimal("50");

    // =========================================================
    // 1. HỆ THỐNG MAP & FARM TÀI NGUYÊN (EXPLORE)
    // =========================================================

    private List<String> getMapResources(int level) {
        if (level < 20) return List.of("Gỗ", "Đá", "Quặng Đồng", "Cá");
        if (level < 30) return List.of("Gỗ", "Đá", "Quặng Đồng", "Sắt", "Cá");
        if (level < 40) return List.of("Gỗ", "Đá", "Quặng Đồng", "Sắt");
        if (level < 50) return List.of("Gỗ", "Đá", "Quặng Đồng", "Sắt", "Bạch Kim");
        if (level < 60) return List.of("Gỗ", "Sắt", "Bạch Kim");
        return List.of("Gỗ", "Cá");
    }

    public Map<String, Object> explore(Integer userId) {
        Character character = getCharacter(userId);
        Map<String, Object> result = new HashMap<>();
        List<String> logs = new ArrayList<>();

        // 1. Cộng EXP
        int expGain = 15;
        character.setCurrentExp(character.getCurrentExp() + expGain);

        // Check lên cấp đơn giản
        if (character.getCurrentExp() >= character.getLevel() * 100) {
            character.setCurrentExp(0);
            character.setLevel(character.getLevel() + 1);
            // Tăng stat khi lên cấp
            character.setMaxHp(character.getMaxHp() + 50);
            character.setCurrentHp(character.getMaxHp());
            logs.add("🎉 LÊN CẤP! Cấp độ hiện tại: " + character.getLevel());
        }

        logs.add("Bạn đi thám hiểm... (+ " + expGain + " EXP)");

        // 2. Logic rớt nguyên liệu (70%)
        if (random.nextInt(100) < 70) {
            List<String> possibleDrops = getMapResources(character.getLevel());
            String dropName = possibleDrops.get(random.nextInt(possibleDrops.size()));

            Item matItem = itemRepo.findByName(dropName).orElse(null);

            if (matItem != null) {
                UserItem ui = userItemRepo.findByUser_UserIdAndItem_ItemId(userId, matItem.getItemId())
                        .orElse(new UserItem());

                // [FIXED] Sửa lỗi getId() -> getUserItemId()
                if (ui.getUserItemId() == null) {
                    ui.setUser(character.getUser());
                    ui.setItem(matItem);
                    ui.setQuantity(0);
                    ui.setIsEquipped(false);
                    ui.setEnhanceLevel(0);
                    ui.setMainStatValue(BigDecimal.ZERO); // Tránh null
                    ui.setRarity(Rarity.COMMON);
                }

                ui.setQuantity(ui.getQuantity() + 1);
                userItemRepo.save(ui);
                logs.add("🎒 Nhặt được: " + dropName);
            } else {
                // logs.add("Thấy " + dropName + " nhưng chưa có trong DB Items");
            }
        } else {
            logs.add("Không tìm thấy gì đặc biệt.");
        }

        charRepo.save(character);
        result.put("logs", logs);
        result.put("playerExp", character.getCurrentExp());
        result.put("playerLevel", character.getLevel());
        return result;
    }

    // =========================================================
    // 2. HỆ THỐNG CƯỜNG HÓA (+1 -> +30)
    // =========================================================

    private String getRequiredMaterial(int currentEnhanceLevel) {
        if (currentEnhanceLevel < 10) return "Gỗ";
        if (currentEnhanceLevel < 20) return "Sắt";
        return "Bạch Kim";
    }

    public Map<String, Object> enhanceItem(Integer userId, Long userItemId) {
        Map<String, Object> result = new HashMap<>();
        UserItem item = userItemRepo.findById(userItemId)
                .orElseThrow(() -> new RuntimeException("Item ko tồn tại"));

        if (!item.getUser().getUserId().equals(userId)) throw new RuntimeException("Item không chính chủ");
        if (item.getEnhanceLevel() >= 30) throw new RuntimeException("Đã đạt cấp tối đa (+30)");

        // 1. Kiểm tra nguyên liệu
        String matName = getRequiredMaterial(item.getEnhanceLevel());
        int qtyRequired = (item.getEnhanceLevel() / 5) + 1;

        UserItem materialInBag = userItemRepo.findByUser_UserIdAndItem_Name(userId, matName)
                .orElseThrow(() -> new RuntimeException("Thiếu nguyên liệu! Cần " + qtyRequired + " cái " + matName));

        if (materialInBag.getQuantity() < qtyRequired) {
            throw new RuntimeException("Không đủ " + matName + "! (Có: " + materialInBag.getQuantity() + ", Cần: " + qtyRequired + ")");
        }

        // 2. Trừ nguyên liệu
        materialInBag.setQuantity(materialInBag.getQuantity() - qtyRequired);
        if (materialInBag.getQuantity() <= 0) {
            userItemRepo.delete(materialInBag);
        } else {
            userItemRepo.save(materialInBag);
        }

        // 3. Tăng cấp (+1)
        item.setEnhanceLevel(item.getEnhanceLevel() + 1);

        // Tăng Main Stat
        if (item.getMainStatValue() == null) item.setMainStatValue(BigDecimal.TEN);

        BigDecimal base = item.getMainStatValue().divide(BigDecimal.valueOf(1.0 + (item.getEnhanceLevel()-1)*0.05), 2, BigDecimal.ROUND_HALF_UP);
        BigDecimal newVal = base.multiply(BigDecimal.valueOf(1.0 + item.getEnhanceLevel() * 0.05));
        item.setMainStatValue(newVal);

        List<String> logs = new ArrayList<>();
        logs.add("Đập thành công lên +" + item.getEnhanceLevel() + "! (Tốn " + qtyRequired + " " + matName + ")");

        // 4. Nhảy dòng (Mỗi 3 cấp)
        if (item.getEnhanceLevel() % 3 == 0) {
            handleSubStatRoll(item, logs);
        }

        userItemRepo.save(item);
        result.put("success", true);
        result.put("item", item);
        result.put("logs", logs);
        return result;
    }

    private void handleSubStatRoll(UserItem item, List<String> logs) {
        try {
            List<SubStatDTO> subs = new ArrayList<>();
            if (item.getSubStats() != null && !item.getSubStats().equals("[]")) {
                subs = objectMapper.readValue(item.getSubStats(), new TypeReference<List<SubStatDTO>>() {});
            }

            if (subs.size() < 4) {
                SubStatDTO newSub = itemGenService.generateRandomSubStat(item, subs);
                subs.add(newSub);
                logs.add("✨ Kích hoạt dòng mới: " + newSub.getType());
            } else {
                int idx = random.nextInt(subs.size());
                SubStatDTO target = subs.get(idx);
                double bonus = getBonusValue(target.getType());
                target.setValue(target.getValue() + bonus);
                logs.add("🔥 Dòng [" + target.getType() + "] được cường hóa thêm +" + (int)bonus);
            }
            item.setSubStats(objectMapper.writeValueAsString(subs));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private double getBonusValue(String type) {
        if (type.equals("SPEED")) return random.nextInt(3) + 2;
        if (type.contains("PERCENT")) return random.nextInt(5) + 4;
        return random.nextInt(20) + 10;
    }

    // =========================================================
    // 3. CÁC CHỨC NĂNG CƠ BẢN (KHÔI PHỤC LOGIC)
    // =========================================================

    public User getPlayerOrCreate(Integer userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        if (user.getCharacter() == null) {
            user.setCharacter(characterService.createDefaultCharacter(user));
            return userRepo.save(user);
        }
        return user;
    }

    private Character getCharacter(Integer userId) {
        User user = userRepo.findById(userId).orElseThrow();
        if (user.getCharacter() == null) {
            characterService.createDefaultCharacter(user);
            return charRepo.findByUser_UserId(userId).orElseThrow();
        }
        return user.getCharacter();
    }

    public User restAtInn(Integer userId) {
        Character character = getCharacter(userId);
        Wallet wallet = character.getUser().getWallet();

        if (wallet.getGold().compareTo(REST_COST) < 0) {
            throw new RuntimeException("Không đủ 50 vàng để nghỉ trọ!");
        }

        wallet.setGold(wallet.getGold().subtract(REST_COST));
        walletRepo.save(wallet);

        character.setCurrentHp(character.getMaxHp());
        character.setCurrentEnergy(character.getMaxEnergy());
        return charRepo.save(character).getUser();
    }

    public List<UserItem> getInventory(Integer userId) {
        return userItemRepo.findByUser_UserId(userId);
    }

    public Map<String, Object> equipItem(Integer userId, Long userItemId) {
        Character character = getCharacter(userId);
        UserItem itemToEquip = userItemRepo.findById(userItemId).orElseThrow();
        Map<String, Object> result = new HashMap<>();

        if (!itemToEquip.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Vật phẩm không chính chủ");
        }

        // Tháo đồ cũ cùng loại
        String type = itemToEquip.getItem().getType();
        userItemRepo.findByUser_UserIdAndItem_TypeAndIsEquippedTrue(userId, type)
                .ifPresent(oldItem -> {
                    oldItem.setIsEquipped(false);
                    userItemRepo.save(oldItem);
                });

        // Mặc đồ mới
        itemToEquip.setIsEquipped(true);
        userItemRepo.save(itemToEquip);

        result.put("success", true);
        result.put("character", character);
        return result;
    }

    public Map<String, Object> unequipItem(Integer userId, Long userItemId) {
        Character character = getCharacter(userId);
        UserItem item = userItemRepo.findById(userItemId).orElseThrow();
        Map<String, Object> result = new HashMap<>();

        if (item.getUser().getUserId().equals(userId) && item.getIsEquipped()) {
            item.setIsEquipped(false);
            userItemRepo.save(item);
        }

        result.put("success", true);
        result.put("character", character);
        return result;
    }
}