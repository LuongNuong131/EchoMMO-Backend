package com.echommo.service;

import com.echommo.entity.Character;
import com.echommo.entity.Item;
import com.echommo.entity.UserItem;
import com.echommo.enums.Role; // [ADD] Import Role
import com.echommo.repository.CharacterRepository;
import com.echommo.repository.UserItemRepository;
import com.echommo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class InventoryService {

    @Autowired
    private UserItemRepository userItemRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private CharacterRepository charRepo;

    // Lấy danh sách đồ
    public List<UserItem> getInventory(Integer userId) {
        return userItemRepo.findByUser_UserId(userId);
    }

    // --- MẶC ĐỒ ---
    @Transactional
    public void equipItem(Integer userId, Integer userItemId) {
        UserItem newItem = userItemRepo.findById(userItemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (!newItem.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("This item does not belong to you!");
        }

        String itemType = newItem.getItem().getType();
        Optional<UserItem> currentEquipped = userItemRepo
                .findByUser_UserIdAndItem_TypeAndIsEquippedTrue(userId, itemType);

        if (currentEquipped.isPresent()) {
            UserItem oldItem = currentEquipped.get();
            if (!oldItem.getUserItemId().equals(newItem.getUserItemId())) {
                oldItem.setIsEquipped(false);
                userItemRepo.save(oldItem);
            }
        }

        newItem.setIsEquipped(true);
        userItemRepo.save(newItem);

        recalculateCharacterStats(userId);
    }

    // --- THÁO ĐỒ ---
    @Transactional
    public void unequipItem(Integer userId, Integer userItemId) {
        UserItem item = userItemRepo.findById(userItemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (!item.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Not your item");
        }

        item.setIsEquipped(false);
        userItemRepo.save(item);

        recalculateCharacterStats(userId);
    }

    // --- LOGIC TÍNH CHỈ SỐ (CHECK ROLE ADMIN) ---
    private void recalculateCharacterStats(Integer userId) {
        Character character = charRepo.findByUser_UserId(userId)
                .orElseThrow(() -> new RuntimeException("Character not found"));

        // [FIX] KIỂM TRA ROLE ADMIN
        boolean isAdmin = character.getUser().getRole() == Role.ADMIN;

        // 1. Xác định mốc khởi điểm (Base)
        int totalAtk = isAdmin ? 999 : 0;
        int totalDef = isAdmin ? 999 : 0;
        int totalSpeed = isAdmin ? 999 : 0;
        int totalCritRate = isAdmin ? 100 : 1;    // Admin 100% crit
        int totalCritDmg = isAdmin ? 300 : 150;   // Admin 300% crit dmg

        int totalMaxHp = isAdmin ? 9999 : 100;    // Admin máu trâu
        int totalMaxEnergy = isAdmin ? 999 : 50;

        // 2. Cộng dồn đồ đang mặc
        List<UserItem> inventory = userItemRepo.findByUser_UserId(userId);

        for (UserItem ui : inventory) {
            if (Boolean.TRUE.equals(ui.getIsEquipped())) {
                Item item = ui.getItem();

                if (item.getAtkBonus() != null) totalAtk += item.getAtkBonus();
                if (item.getDefBonus() != null) totalDef += item.getDefBonus();
                if (item.getSpeedBonus() != null) totalSpeed += item.getSpeedBonus();
                if (item.getCritRateBonus() != null) totalCritRate += item.getCritRateBonus();

                if (item.getHpBonus() != null) totalMaxHp += item.getHpBonus();
                if (item.getEnergyBonus() != null) totalMaxEnergy += item.getEnergyBonus();
            }
        }

        // 3. Update vào DB
        character.setBaseAtk(totalAtk);
        character.setBaseDef(totalDef);
        character.setBaseSpeed(totalSpeed);
        character.setBaseCritRate(totalCritRate);
        character.setBaseCritDmg(totalCritDmg);

        character.setMaxHp(totalMaxHp);
        character.setMaxEnergy(totalMaxEnergy);

        // 4. Fix tràn máu
        if (character.getHp() > character.getMaxHp()) {
            character.setHp(character.getMaxHp());
        }
        if (character.getEnergy() > character.getMaxEnergy()) {
            character.setEnergy(character.getMaxEnergy());
        }

        charRepo.save(character);
    }
}