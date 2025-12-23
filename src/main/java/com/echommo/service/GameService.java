package com.echommo.service;

import com.echommo.entity.Item;
import com.echommo.entity.User;
import com.echommo.entity.UserItem;
import com.echommo.entity.Character;
import com.echommo.enums.Rarity;
import com.echommo.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@Transactional
public class GameService {

    @Autowired private UserRepository userRepo;
    @Autowired private CharacterRepository charRepo;
    @Autowired private UserItemRepository userItemRepo;
    @Autowired private ItemRepository itemRepo;
    @Autowired private CharacterService characterService;

    private final Random random = new Random();

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Lỗi xác thực: Người dùng chưa đăng nhập."));
    }

    private Character getCharacter(Integer userId) {
        return charRepo.findByUser_UserId(userId)
                .orElseGet(() -> {
                    User user = userRepo.findById(userId)
                            .orElseThrow(() -> new EntityNotFoundException("User not found"));
                    return characterService.createDefaultCharacter(user);
                });
    }

    // [LƯU Ý] Tên resource ở đây phải khớp với cột 'name' trong bảng 'items'
    private List<String> getMapResources(int level) {
        if (level < 20) return List.of("Gỗ", "Đá", "Quặng Đồng", "Cá");
        if (level < 30) return List.of("Gỗ", "Đá", "Quặng Đồng", "Sắt", "Cá");
        if (level < 40) return List.of("Gỗ", "Đá", "Quặng Đồng", "Sắt");
        if (level < 50) return List.of("Gỗ", "Đá", "Quặng Đồng", "Sắt", "Bạch Kim");
        return List.of("Gỗ", "Cá");
    }

    public Map<String, Object> explore(Integer userId) {
        Character character = getCharacter(userId);
        Map<String, Object> result = new HashMap<>();
        List<String> logs = new ArrayList<>();

        int expGain = 15;
        character.setCurrentExp(character.getCurrentExp() + expGain);

        // Logic lên cấp
        if (character.getCurrentExp() >= character.getLevel() * 100L) {
            character.setCurrentExp(0L);
            character.setLevel(character.getLevel() + 1);
            character.setMaxHp(character.getMaxHp() + 50);
            character.setCurrentHp(character.getMaxHp());

            // [FIX] Gọi hàm tính lại chỉ số để đồng bộ stat point
            characterService.recalculateStats(character);

            logs.add("🎉 LÊN CẤP! Cấp độ hiện tại: " + character.getLevel());
        }
        logs.add("Bạn đi thám hiểm... (+ " + expGain + " EXP)");

        // Tỷ lệ rơi đồ 70%
        if (random.nextInt(100) < 70) {
            List<String> possibleDrops = getMapResources(character.getLevel());
            String dropName = possibleDrops.get(random.nextInt(possibleDrops.size()));

            // Tìm item theo tên (Lưu ý: Database phải có item tên này)
            Item matItem = itemRepo.findByName(dropName).orElse(null);

            if (matItem != null) {
                // [FIX] Dùng list stream để an toàn hơn, tránh lỗi NonUniqueResultException
                UserItem ui = userItemRepo.findByCharacter_CharIdAndItem_ItemId(character.getCharId(), matItem.getItemId())
                        .stream()
                        .filter(item -> !item.getIsEquipped()) // Chỉ stack vào item không trang bị
                        .findFirst()
                        .orElse(null);

                if (ui == null) {
                    ui = new UserItem();
                    ui.setCharacter(character);
                    ui.setItem(matItem);
                    ui.setQuantity(0);
                    ui.setIsEquipped(false);
                    ui.setEnhanceLevel(0);
                    ui.setAcquiredAt(LocalDateTime.now());
                    ui.setMainStatValue(BigDecimal.ZERO);
                    ui.setRarity(Rarity.COMMON);
                    ui.setSubStats("[]");

                    // [FIX QUAN TRỌNG] Khởi tạo các trường mới của DB để tránh lỗi NULL
                    ui.setOriginalMainStatValue(BigDecimal.ZERO);
                    ui.setMythicStars(0);
                    ui.setVisualVariant(0);
                    ui.setIsMythic(false);
                    ui.setCurrentDurability(100);
                    ui.setMaxDurability(100);
                }

                ui.setQuantity(ui.getQuantity() + 1);
                userItemRepo.save(ui);
                logs.add("🎒 Nhặt được: " + dropName);
            } else {
                // [DEBUG] Log nếu không tìm thấy item trong DB
                // logs.add("Tìm thấy " + dropName + " nhưng chưa có trong DB items.");
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

    public List<UserItem> getInventory(Integer userId) {
        Character character = getCharacter(userId);
        return userItemRepo.findByCharacter_CharIdOrderByAcquiredAtDesc(character.getCharId());
    }
}