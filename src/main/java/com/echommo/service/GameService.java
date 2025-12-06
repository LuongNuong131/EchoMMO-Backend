package com.echommo.service;

import com.echommo.entity.Character;
import com.echommo.entity.Item;
import com.echommo.entity.User;
import com.echommo.entity.Wallet;
import com.echommo.repository.CharacterRepository;
import com.echommo.repository.ItemRepository;
import com.echommo.repository.UserRepository;
import com.echommo.repository.WalletRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class GameService {

    @Autowired private UserRepository userRepo;
    @Autowired private CharacterRepository charRepo;
    @Autowired private ItemRepository itemRepo;
    @Autowired private WalletRepository walletRepo;

    // Cấu hình game
    // private static final int EXPLORE_ENERGY_COST = 2; // XÓA DÒNG NÀY HOẶC COMMENT LẠI
    private static final int ATTACK_ENERGY_COST = 2;
    private static final int STRONG_ATTACK_ENERGY_COST = 5;
    private static final long BASE_EXP_REQUIREMENT = 100L;
    private static final BigDecimal REST_COST = new BigDecimal("50");

    public User getPlayerOrCreate(Integer userId) {
        return userRepo.findById(userId).map(user -> {
            if (user.getCharacter() == null) {
                Character newChar = new Character();
                newChar.setUser(user);
                user.setCharacter(newChar);
                userRepo.save(user);
            }
            return user;
        }).orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
    }

    private Character getCharacter(Integer userId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (user.getCharacter() == null) {
            throw new IllegalStateException("Character data missing for user: " + userId);
        }
        return user.getCharacter();
    }

    private String checkLevelUp(Character character) {
        long requiredExp = BASE_EXP_REQUIREMENT * (long) Math.pow(character.getLv(), 2);
        if (character.getExp() >= requiredExp) {
            character.setExp((int)(character.getExp() - requiredExp));
            character.setLv(character.getLv() + 1);
            character.setBaseAtk(character.getBaseAtk() + 2);
            character.setBaseDef(character.getBaseDef() + 1);
            character.setMaxHp(character.getMaxHp() + 20);
            character.setMaxEnergy(character.getMaxEnergy() + 5);
            character.setHp(character.getMaxHp());
            character.setEnergy(character.getMaxEnergy());
            charRepo.save(character);
            return "LEVEL UP! Cấp " + character.getLv();
        }
        return null;
    }

    public Map<String, Object> attackEnemy(Integer userId, boolean isParried, String attackType) {
        Character character = getCharacter(userId);
        Map<String, Object> result = new HashMap<>();

        int energyCost = "strong".equals(attackType) ? STRONG_ATTACK_ENERGY_COST : ATTACK_ENERGY_COST;
        if (character.getEnergy() < energyCost) {
            result.put("status", "FAIL");
            result.put("message", "Năng lượng không đủ để tấn công.");
            return result;
        }
        character.setEnergy(character.getEnergy() - energyCost);

        int enemyAtk = 15 + character.getLv();
        int expReward = 35;
        double damageMultiplier = "strong".equals(attackType) ? 2.0 : 1.0;

        int damageDealt = (int) (character.getBaseAtk() * (0.9 + Math.random() * 0.2) * damageMultiplier);
        int damageTaken = isParried ? 0 : Math.max(1, enemyAtk - (character.getBaseDef() / 2));

        int currentHp = Math.max(0, character.getHp() - damageTaken);
        character.setHp(currentHp);

        String message;
        if (currentHp == 0) {
            result.put("status", "DIED");
            message = "💀 Thất bại...";
            character.setHp(character.getMaxHp() / 2);
        } else {
            result.put("status", "ALIVE");
            character.setExp(character.getExp() + expReward);
            message = "Trúng! (+" + expReward + " EXP)";
            String lvUpMsg = checkLevelUp(character);
            if (lvUpMsg != null) {
                message = lvUpMsg;
                result.put("levelUp", true);
            }
        }

        charRepo.save(character);
        result.put("message", isParried ? "🛡️ PARRY! " + message : message);
        result.put("damageDealt", damageDealt);
        result.put("damageTaken", damageTaken);
        result.put("playerHp", character.getHp());
        result.put("playerEnergy", character.getEnergy());
        result.put("playerExp", character.getExp());
        result.put("playerLv", character.getLv());
        result.put("nextLevelExp", BASE_EXP_REQUIREMENT * (long) Math.pow(character.getLv(), 2));

        return result;
    }

    // [UPDATED] Xóa logic trừ năng lượng
    public Map<String, Object> explore(Integer userId) {
        Character character = getCharacter(userId);
        Map<String, Object> result = new HashMap<>();

        // --- BỎ ĐOẠN CHECK ENERGY ---
        // if (character.getEnergy() < EXPLORE_ENERGY_COST) { ... }
        // character.setEnergy(character.getEnergy() - EXPLORE_ENERGY_COST);

        int expGain = 10;
        character.setExp(character.getExp() + expGain);
        String lvMsg = checkLevelUp(character);
        boolean isLevelUp = (lvMsg != null);

        double roll = Math.random() * 100;
        String message = "";

        if (roll < 40) {
            result.put("type", "NOTHING");
            message = "Khu rừng yên tĩnh...";
        } else if (roll < 70) {
            int goldGain = (int) (Math.random() * 20) + 10;
            Wallet w = character.getUser().getWallet();
            w.setGold(w.getGold().add(new BigDecimal(goldGain)));
            walletRepo.save(w);

            result.put("type", "GOLD");
            message = "Nhặt được " + goldGain + " vàng!";
        } else {
            result.put("type", "ENEMY");
            message = "Quái vật xuất hiện!";
        }

        message += " (+" + expGain + " EXP)";
        if(isLevelUp) message += " [LÊN CẤP!]";

        charRepo.save(character);
        result.put("message", message);
        result.put("playerExp", character.getExp());
        result.put("playerLv", character.getLv());
        result.put("playerEnergy", character.getEnergy());
        result.put("nextLevelExp", BASE_EXP_REQUIREMENT * (long) Math.pow(character.getLv(), 2));

        return result;
    }

    public User restAtInn(Integer userId) {
        Character character = getCharacter(userId);
        User user = character.getUser();
        Wallet wallet = user.getWallet();

        if (wallet.getGold().compareTo(REST_COST) < 0) {
            throw new RuntimeException("Không đủ ngân lượng! Cần " + REST_COST + " Vàng.");
        }

        wallet.setGold(wallet.getGold().subtract(REST_COST));
        walletRepo.save(wallet);

        character.setHp(character.getMaxHp());
        character.setEnergy(character.getMaxEnergy());
        charRepo.save(character);

        return user;
    }

    public List<Item> getInventory(Integer userId) {
        return itemRepo.findByUser_UserId(userId);
    }

    public Map<String, Object> equipItem(Integer userId, Integer itemId) {
        Character character = getCharacter(userId);
        Item itemToEquip = itemRepo.findById(itemId).orElseThrow();
        Map<String, Object> result = new HashMap<>();

        if (!itemToEquip.getUser().getUserId().equals(userId)) {
            result.put("success", false);
            return result;
        }

        Item currentItem = itemRepo.findByUser_UserIdAndTypeAndIsEquippedTrue(userId, itemToEquip.getType());
        if (currentItem != null) {
            currentItem.setIsEquipped(false);
            character.setBaseAtk(character.getBaseAtk() - currentItem.getAtkBonus());
            character.setBaseDef(character.getBaseDef() - currentItem.getDefBonus());
            character.setMaxHp(character.getMaxHp() - currentItem.getHpBonus());
            character.setMaxEnergy(character.getMaxEnergy() - currentItem.getEnergyBonus());
            itemRepo.save(currentItem);
        }

        itemToEquip.setIsEquipped(true);
        character.setBaseAtk(character.getBaseAtk() + itemToEquip.getAtkBonus());
        character.setBaseDef(character.getBaseDef() + itemToEquip.getDefBonus());
        character.setMaxHp(character.getMaxHp() + itemToEquip.getHpBonus());
        character.setMaxEnergy(character.getMaxEnergy() + itemToEquip.getEnergyBonus());

        if (character.getHp() > character.getMaxHp()) character.setHp(character.getMaxHp());
        if (character.getEnergy() > character.getMaxEnergy()) character.setEnergy(character.getMaxEnergy());

        itemRepo.save(itemToEquip);
        charRepo.save(character);

        result.put("success", true);
        result.put("character", character);
        return result;
    }

    public Map<String, Object> unequipItem(Integer userId, Integer itemId) {
        Character character = getCharacter(userId);
        Item item = itemRepo.findById(itemId).orElseThrow();
        Map<String, Object> result = new HashMap<>();

        if (!item.getUser().getUserId().equals(userId) || !item.getIsEquipped()) {
            result.put("success", false);
            return result;
        }

        item.setIsEquipped(false);
        character.setBaseAtk(character.getBaseAtk() - item.getAtkBonus());
        character.setBaseDef(character.getBaseDef() - item.getDefBonus());
        character.setMaxHp(character.getMaxHp() - item.getHpBonus());
        character.setMaxEnergy(character.getMaxEnergy() - item.getEnergyBonus());

        if (character.getHp() > character.getMaxHp()) character.setHp(character.getMaxHp());
        if (character.getEnergy() > character.getMaxEnergy()) character.setEnergy(character.getMaxEnergy());

        itemRepo.save(item);
        charRepo.save(character);

        result.put("success", true);
        result.put("character", character);
        return result;
    }
}