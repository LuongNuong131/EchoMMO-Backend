package com.echommo.service;

import com.echommo.entity.Item;
import com.echommo.entity.User;
import com.echommo.entity.UserItem;
import com.echommo.entity.Wallet;
import com.echommo.entity.Character;
import com.echommo.enums.Rarity;
import com.echommo.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@Transactional
public class GameService {

    @Autowired private UserRepository userRepo;
    @Autowired private CharacterRepository charRepo;
    @Autowired private WalletRepository walletRepo;
    @Autowired private UserItemRepository userItemRepo;
    @Autowired private ItemRepository itemRepo;
    @Autowired private CharacterService characterService;
    @Autowired private EquipmentService equipmentService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();
    // [LƯU Ý]: Hằng số REST_COST và REST_COOLDOWN_SECONDS đã được loại bỏ

    // --- HELPER METHODS: CURRENT USER (FIXED LOGIC) ---
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        if (username == null || username.equals("anonymousUser")) {
            throw new RuntimeException("Lỗi xác thực: Người dùng chưa đăng nhập hoặc token đã hết hạn.");
        }

        return userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Lỗi CSDL: Không tìm thấy người dùng [" + username + "] trong hệ thống."));
    }

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

                if (ui.getUserItemId() == null) {
                    ui.setUser(character.getUser());
                    ui.setItem(matItem);
                    ui.setQuantity(0);
                    ui.setIsEquipped(false);
                    ui.setEnhanceLevel(0);
                    ui.setMainStatValue(BigDecimal.ZERO);
                }

                ui.setQuantity(ui.getQuantity() + 1);
                userItemRepo.save(ui);
                logs.add("🎒 Nhặt được: " + dropName);
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
    // 2. CÁC CHỨC NĂNG CƠ BẢN
    // [LƯU Ý]: Phương thức restAtInn đã được loại bỏ
    // =========================================================

    // --- HELPER METHODS ---
    private Character getCharacter(Integer userId) {
        // FIX: Sử dụng phương thức JOIN FETCH để tối ưu DB call
        Optional<Character> optionalCharacter = charRepo.findByUser_UserIdWithUserAndWallet(userId);

        if (optionalCharacter.isPresent()) {
            return optionalCharacter.get();
        }

        // Nếu không tìm thấy, quay lại logic gốc: Fetch User và tạo Character nếu cần
        User user = userRepo.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (user.getCharacter() == null) {
            characterService.createDefaultCharacter(user);
            // Fetch lại bằng query tối ưu sau khi tạo
            return charRepo.findByUser_UserIdWithUserAndWallet(userId).orElseThrow();
        }

        throw new EntityNotFoundException("Character not found for User: " + userId);
    }

    public List<UserItem> getInventory(Integer userId) {
        return userItemRepo.findByUser_UserId(userId);
    }

    public Map<String, Object> equipItem(Integer userId, Long userItemId) {
        return Map.of("success", true);
    }

    public Map<String, Object> unequipItem(Integer userId, Long userItemId) {
        return Map.of("success", true);
    }

    public User getPlayerOrCreate(Integer userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }
}