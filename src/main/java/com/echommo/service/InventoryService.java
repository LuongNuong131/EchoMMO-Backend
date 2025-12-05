package com.echommo.service;

import com.echommo.entity.*;
import com.echommo.entity.Character;
import com.echommo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class InventoryService {

    @Autowired private UserItemRepository userItemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CharacterRepository characterRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private ItemRepository itemRepository;

    public List<UserItem> getMyInventory() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        Wallet wallet = user.getWallet();

        List<UserItem> inventory = userItemRepository.findByUser_UserIdOrderByIsEquippedDesc(user.getUserId());
        List<UserItem> displayList = new ArrayList<>(inventory);

        // Tạo item ảo từ Wallet (Gỗ/Đá)
        if (wallet.getWood() > 0) createVirtualItem(user, "Gỗ Sồi", wallet.getWood(), displayList);
        if (wallet.getStone() > 0) createVirtualItem(user, "Đá Cứng", wallet.getStone(), displayList);

        return displayList;
    }

    private void createVirtualItem(User user, String itemName, Integer quantity, List<UserItem> list) {
        Optional<Item> itemOpt = itemRepository.findByName(itemName);
        if (itemOpt.isPresent()) {
            UserItem dummy = new UserItem();
            // Cần sử dụng các trường của UserItem, không phải Item
            // LƯU Ý: UserItem entity không có trường name hay type, nó lấy từ Item.
            // Đoạn này chỉ tạo đối tượng ảo (dummy) để hiển thị
            dummy.setUserItemId(-new Random().nextInt(10000));
            dummy.setUser(user);
            dummy.setItem(itemOpt.get());
            dummy.setQuantity(quantity);
            dummy.setIsEquipped(false);
            dummy.setEnhanceLevel(0);
            list.add(dummy);
        }
    }

    // --- SỬ DỤNG VẬT PHẨM ---
    @Transactional
    public String useItem(Integer userItemId) {
        UserItem item = userItemRepository.findById(userItemId)
                .orElseThrow(() -> new RuntimeException("Vật phẩm không tồn tại"));

        if (!"CONSUMABLE".equals(item.getItem().getType())) {
            throw new RuntimeException("Vật phẩm này không thể sử dụng!");
        }

        Character character = characterRepository.findByUser_UserId(item.getUser().getUserId())
                .orElseThrow(() -> new RuntimeException("Chưa tạo nhân vật"));

        // Hiệu ứng (Ví dụ: Bình máu)
        if (item.getItem().getName().contains("Máu") || item.getItem().getName().contains("Potion")) {
            int healAmount = 50;
            if (item.getItem().getHpBonus() != null && item.getItem().getHpBonus() > 0) {
                healAmount = item.getItem().getHpBonus();
            }
            // Hồi máu và đảm bảo không vượt quá Max HP
            character.setHp(Math.min(character.getMaxHp(), character.getHp() + healAmount));
            characterRepository.save(character);
        } else {
            return "Vật phẩm này chưa có tác dụng gì!";
        }

        // Trừ số lượng
        if (item.getQuantity() > 1) {
            item.setQuantity(item.getQuantity() - 1);
            userItemRepository.save(item);
        } else {
            userItemRepository.delete(item);
        }

        return "Đã sử dụng " + item.getItem().getName();
    }

    // --- MẶC TRANG BỊ ---
    @Transactional
    public String equipItem(Integer userItemId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        Character character = characterRepository.findByUser_UserId(user.getUserId()).orElseThrow();
        UserItem itemToEquip = userItemRepository.findById(userItemId).orElseThrow(() -> new RuntimeException("Item không tồn tại"));

        if (!itemToEquip.getUser().getUserId().equals(user.getUserId())) throw new RuntimeException("Không phải đồ của bạn");
        if (itemToEquip.getIsEquipped()) return "Đã trang bị rồi";

        String type = itemToEquip.getItem().getType();
        List<UserItem> inventory = userItemRepository.findByUser_UserIdOrderByIsEquippedDesc(user.getUserId());
        for (UserItem current : inventory) {
            if (current.getIsEquipped() && current.getItem().getType().equals(type)) {
                unequipLogic(current, character);
            }
        }

        itemToEquip.setIsEquipped(true);
        updateStats(character, itemToEquip, true); // Cộng chỉ số

        userItemRepository.save(itemToEquip);
        characterRepository.save(character);
        return "Đã trang bị: " + itemToEquip.getItem().getName();
    }

    // --- GỠ TRANG BỊ ---
    @Transactional
    public String unequipItem(Integer userItemId) {
        UserItem item = userItemRepository.findById(userItemId).orElseThrow();
        if (!item.getIsEquipped()) return "Chưa trang bị";
        Character character = characterRepository.findByUser_UserId(item.getUser().getUserId()).orElseThrow();

        unequipLogic(item, character); // Trừ chỉ số
        // Lệnh save đã được gọi trong unequipLogic, nhưng gọi lại để đảm bảo Character được lưu
        characterRepository.save(character);
        return "Đã tháo: " + item.getItem().getName();
    }

    // --- LOGIC CHUNG GỠ ĐỒ ---
    private void unequipLogic(UserItem item, Character character) {
        item.setIsEquipped(false);
        updateStats(character, item, false); // Trừ chỉ số
        userItemRepository.save(item);
        // Sau khi trừ stats, updateStats sẽ kiểm tra tính toàn vẹn HP/Energy
    }

    // --- LOGIC CHUNG CỘNG/TRỪ CHỈ SỐ ---
    private void updateStats(Character character, UserItem item, boolean isEquip) {
        int sign = isEquip ? 1 : -1;

        // Tính toán Bonus sau khi Enhance (Sử dụng Base Stats từ Item)
        int bonusAtk = item.getItem().getAtkBonus() + (item.getEnhanceLevel() * 2);
        int bonusDef = item.getItem().getDefBonus() + (item.getEnhanceLevel() * 1);

        // FIX: Sửa lỗi Setter/Getter - Phải dùng setBaseAtk/getBaseAtk
        character.setBaseAtk(character.getBaseAtk() + (sign * bonusAtk));
        character.setBaseDef(character.getBaseDef() + (sign * bonusDef));
        character.setMaxHp(character.getMaxHp() + (sign * item.getItem().getHpBonus()));
        character.setBaseSpeed(character.getBaseSpeed() + (sign * item.getItem().getSpeedBonus()));
        character.setBaseCritRate(character.getBaseCritRate() + (sign * item.getItem().getCritRateBonus()));

        // LƯU Ý: Item Entity cần phải có getEnergyBonus để dòng sau hoạt động
        // character.setMaxEnergy(character.getMaxEnergy() + (sign * item.getItem().getEnergyBonus()));

        // --- FIX LOGIC TÍNH TOÀN VẸN CHỈ SỐ (CRITICAL) ---

        // 1. Đảm bảo ATK/DEF không xuống dưới 0 (Hoặc mức Base tối thiểu)
        if (character.getBaseAtk() < 1) character.setBaseAtk(1);
        if (character.getBaseDef() < 0) character.setBaseDef(0);

        // 2. Kiểm tra HP/Energy hiện tại không vượt quá Max mới (Xảy ra khi gỡ đồ)
        if (character.getHp() > character.getMaxHp()) character.setHp(character.getMaxHp());
        if (character.getEnergy() > character.getMaxEnergy()) character.setEnergy(character.getMaxEnergy());
    }

    // --- NÂNG CẤP TRANG BỊ ---
    @Transactional
    public String enhanceItem(Integer userItemId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        Wallet wallet = user.getWallet();
        UserItem item = userItemRepository.findById(userItemId).orElseThrow();
        Character character = characterRepository.findByUser_UserId(user.getUserId()).orElseThrow(); // Lấy Character trước khi nâng cấp

        if (item.getEnhanceLevel() >= 10) throw new RuntimeException("Đã đạt cấp tối đa (+10)");

        BigDecimal goldCost = new BigDecimal((item.getEnhanceLevel() + 1) * 50);
        int resourceCost = (item.getEnhanceLevel() + 1) * 2;

        if (wallet.getGold().compareTo(goldCost) < 0) throw new RuntimeException("Thiếu Vàng! Cần " + goldCost);

        boolean needWood = "WEAPON".equals(item.getItem().getType());
        if (needWood) {
            if (wallet.getWood() < resourceCost) throw new RuntimeException("Thiếu Gỗ! Cần " + resourceCost);
            wallet.setWood(wallet.getWood() - resourceCost);
        } else {
            if (wallet.getStone() < resourceCost) throw new RuntimeException("Thiếu Đá! Cần " + resourceCost);
            wallet.setStone(wallet.getStone() - resourceCost);
        }
        wallet.setGold(wallet.getGold().subtract(goldCost));

        int chance = 100 - (item.getEnhanceLevel() * 10);
        boolean success = new Random().nextInt(100) < chance;
        String msg;

        if (success) {
            item.setEnhanceLevel(item.getEnhanceLevel() + 1);
            if (item.getIsEquipped()) {
                // Nếu đang mặc, CẬP NHẬT CHỈ SỐ LÊN NGAY LẬP TỨC
                character.setBaseAtk(character.getBaseAtk() + 2);
                character.setBaseDef(character.getBaseDef() + 1);
                characterRepository.save(character);
            }
            msg = "✅ THÀNH CÔNG! " + item.getItem().getName() + " lên +" + item.getEnhanceLevel();
        } else {
            // Logic reset level khi thất bại (có thể thêm vào sau)
            msg = "❌ THẤT BẠI! Mất nguyên liệu.";
        }

        userItemRepository.save(item);
        walletRepository.save(wallet);
        return msg;
    }
}