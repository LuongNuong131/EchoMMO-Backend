package com.echommo.service.impl;

import com.echommo.entity.*;
import com.echommo.entity.Character;
import com.echommo.enums.Rarity;
import com.echommo.enums.SlotType;
import com.echommo.repository.*;
import com.echommo.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final UserItemRepository userItemRepo;
    private final ItemRepository itemRepo;
    private final CharacterRepository charRepo;
    private final WalletRepository walletRepo;
    private final UserRepository userRepo;
    private final EquipmentService equipmentService;
    private final CharacterService characterService;
    private final ItemGenerationService itemGenService;

    @Override
    public List<UserItem> getInventory(Integer charId) {
        // [FIXED] Sử dụng method mới đã thêm trong Repo
        return userItemRepo.findByCharacter_CharIdAndIsLockedFalseOrderByAcquiredAtDesc(charId);
    }

    @Override
    @Transactional
    public void equipItem(Integer charId, Long userItemId) { // [FIXED] Long
        Character character = charRepo.findById(charId)
                .orElseThrow(() -> new RuntimeException("Character not found"));

        UserItem newItem = userItemRepo.findById(userItemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (!newItem.getCharacter().getCharId().equals(charId)) {
            throw new RuntimeException("Vật phẩm không thuộc về bạn!");
        }

        // [FIXED] Kiểm tra isLocked
        if (Boolean.TRUE.equals(newItem.getIsLocked())) {
            throw new RuntimeException("Vật phẩm đang bị khóa!");
        }

        Item itemBase = newItem.getItem();

        boolean isGear = List.of("WEAPON", "ARMOR", "HELMET", "BOOTS", "RING", "NECKLACE").contains(itemBase.getType());
        boolean isTool = "TOOL".equals(itemBase.getType());

        if (!isGear && !isTool) {
            throw new RuntimeException("Vật phẩm này không thể trang bị!");
        }

        int requiredLv = getRequiredLevel(itemBase.getTier());
        if (character.getLevel() < requiredLv) {
            throw new RuntimeException("Cấp độ không đủ! Cần đạt Level " + requiredLv + " để trang bị.");
        }

        SlotType newSlot = itemBase.getSlotType();
        List<UserItem> equippedItems = userItemRepo.findByCharacter_CharIdAndIsEquippedTrue(charId);

        for (UserItem equipped : equippedItems) {
            if (equipped.getItem().getSlotType() == newSlot) {
                equipped.setIsEquipped(false);
                userItemRepo.save(equipped);
            }
        }

        newItem.setIsEquipped(true);
        userItemRepo.save(newItem);

        characterService.recalculateStats(character);
        charRepo.save(character);
    }

    private int getRequiredLevel(Integer tier) {
        if (tier == null || tier <= 1) return 1;
        switch (tier) {
            case 2: return 10;
            case 3: return 20;
            case 4: return 30;
            case 5: return 50;
            default: return (tier - 1) * 10;
        }
    }

    @Override
    @Transactional
    public void unequipItem(Integer charId, Long userItemId) { // [FIXED] Long
        Character character = charRepo.findById(charId).orElseThrow();
        UserItem item = userItemRepo.findById(userItemId).orElseThrow();

        if (!item.getCharacter().getCharId().equals(charId)) {
            throw new RuntimeException("Vật phẩm không thuộc về bạn!");
        }
        if (!Boolean.TRUE.equals(item.getIsEquipped())) {
            throw new RuntimeException("Vật phẩm này chưa được trang bị!");
        }
        // [FIXED] Check Locked
        if (Boolean.TRUE.equals(item.getIsLocked())) {
            throw new RuntimeException("Vật phẩm đang bị khóa!");
        }

        item.setIsEquipped(false);
        userItemRepo.save(item);

        characterService.recalculateStats(character);
        charRepo.save(character);
    }

    @Override
    @Transactional
    public UserItem enhanceItem(Integer charId, Long userItemId) { // [FIXED] Long
        return equipmentService.enhanceItem(userItemId);
    }

    @Override
    @Transactional
    public UserItem repairItem(User user, Long userItemId) { // [FIXED] Long
        UserItem userItem = userItemRepo.findById(userItemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vật phẩm!"));

        if (!userItem.getCharacter().getUser().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("Vật phẩm không thuộc về bạn!");
        }

        Integer current = userItem.getCurrentDurability();
        Integer max = userItem.getMaxDurability();

        if (current == null) current = 0;
        if (max == null || max <= 0) {
            max = 100;
            userItem.setMaxDurability(100);
        }

        if (current >= max) {
            throw new RuntimeException("Độ bền đã đầy, không cần sửa!");
        }

        int missing = max - current;
        long goldCostPerPoint = 10;
        BigDecimal coinCostPerPoint = BigDecimal.ZERO;

        Rarity rarity = userItem.getRarity();
        if (rarity == null) rarity = userItem.getItem().getRarity();
        if (rarity == null) rarity = Rarity.COMMON;
        if (Boolean.TRUE.equals(userItem.getIsMythic())) rarity = Rarity.MYTHIC;

        switch (rarity) {
            case COMMON: case UNCOMMON: goldCostPerPoint = 10; break;
            case RARE: goldCostPerPoint = 50; break;
            case EPIC: goldCostPerPoint = 200; coinCostPerPoint = BigDecimal.valueOf(0.1); break;
            case LEGENDARY: case MYTHIC: goldCostPerPoint = 1000; coinCostPerPoint = BigDecimal.valueOf(1.0); break;
            default: goldCostPerPoint = 10;
        }

        BigDecimal totalGoldCost = BigDecimal.valueOf(missing * goldCostPerPoint);
        BigDecimal totalCoinCost = coinCostPerPoint.multiply(BigDecimal.valueOf(missing));

        Wallet wallet = user.getWallet();
        if (wallet == null) wallet = walletRepo.findByUser(user).orElseThrow();

        if (wallet.getGold().compareTo(totalGoldCost) < 0) {
            throw new RuntimeException("Không đủ Vàng để sửa! Cần: " + totalGoldCost);
        }
        if (wallet.getEchoCoin().compareTo(totalCoinCost) < 0) {
            throw new RuntimeException("Không đủ Echo Coin để sửa! Cần: " + totalCoinCost);
        }

        wallet.setGold(wallet.getGold().subtract(totalGoldCost));
        wallet.setEchoCoin(wallet.getEchoCoin().subtract(totalCoinCost));
        walletRepo.save(wallet);

        userItem.setCurrentDurability(max);
        return userItemRepo.save(userItem);
    }

    @Override
    @Transactional
    public User expandInventory(User user) {
        int currentSlots = user.getInventorySlots() != null ? user.getInventorySlots() : 49;
        if (currentSlots >= 210) throw new RuntimeException("Kho đồ đã đạt giới hạn tối đa!");

        int nextSlots = currentSlots + 7;
        int costInt = ((currentSlots - 49) / 7) + 1;
        if (costInt < 1) costInt = 1;
        BigDecimal cost = BigDecimal.valueOf(costInt);

        Wallet w = user.getWallet();
        if (w == null) w = walletRepo.findByUser(user).orElseThrow();

        if (w.getEchoCoin().compareTo(cost) < 0) {
            throw new RuntimeException("Thiếu Echo Coin! Cần " + cost);
        }

        w.setEchoCoin(w.getEchoCoin().subtract(cost));
        walletRepo.save(w);

        user.setInventorySlots(nextSlots);
        return userRepo.save(user);
    }

    @Override
    @Transactional
    public void addItemToInventory(User user, Integer itemId, int quantity) {
        Character character = charRepo.findByUser(user).orElseThrow();
        Item item = itemRepo.findById(itemId).orElseThrow();

        int currentCount = userItemRepo.countByCharacter_CharId(character.getCharId());
        int maxSlots = user.getInventorySlots() != null ? user.getInventorySlots() : 49;

        boolean isStackable = List.of("MATERIAL", "CONSUMABLE").contains(item.getType());
        if (!isStackable && currentCount + quantity > maxSlots) throw new RuntimeException("Kho đồ đã đầy!");

        if (isStackable) {
            // [FIXED] Dùng method mới để tìm item chưa bị khóa
            Optional<UserItem> existingItem = userItemRepo.findByCharacter_CharIdAndItem_ItemIdAndIsLockedFalse(character.getCharId(), itemId)
                    .stream().filter(ui -> !Boolean.TRUE.equals(ui.getIsEquipped())).findFirst();
            if (existingItem.isPresent()) {
                UserItem ui = existingItem.get();
                ui.setQuantity(ui.getQuantity() + quantity);
                userItemRepo.save(ui);
                return;
            }
        }

        for (int i = 0; i < quantity; i++) {
            String defaultStatType = getDefaultStatType(item.getType());
            UserItem ui = UserItem.builder()
                    .character(character)
                    .item(item)
                    .quantity(1)
                    .isEquipped(false)
                    .isLocked(false) // [ADDED] Default
                    .enhanceLevel(0)
                    .rarity(item.getRarity() != null ? item.getRarity() : Rarity.COMMON)
                    .acquiredAt(LocalDateTime.now())
                    .mainStatValue(BigDecimal.valueOf(item.getBaseMainStat() != null ? item.getBaseMainStat() : 0))
                    .mainStatType(defaultStatType)
                    .maxDurability(item.getMaxDurability() != null ? item.getMaxDurability() : 100)
                    .currentDurability(item.getMaxDurability() != null ? item.getMaxDurability() : 100)
                    .build();

            if (List.of("WEAPON", "ARMOR", "TOOL", "HELMET", "BOOTS", "RING", "NECKLACE").contains(item.getType())) {
                itemGenService.randomizeNewItem(ui);
            } else {
                ui.setSubStats("[]");
            }
            userItemRepo.save(ui);
        }
    }

    private String getDefaultStatType(String itemType) {
        if (itemType == null) return null;
        switch (itemType) {
            case "WEAPON": return "ATK_FLAT";
            case "ARMOR": case "HELMET": case "BOOTS": return "DEF_FLAT";
            case "RING": return "CRIT_RATE";
            case "NECKLACE": return "HP_FLAT";
            case "TOOL": return "DURABILITY";
            default: return null;
        }
    }
}