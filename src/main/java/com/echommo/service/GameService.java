package com.echommo.service;

import com.echommo.entity.Character;
import com.echommo.entity.Item;
import com.echommo.entity.User;
import com.echommo.repository.CharacterRepository;
import com.echommo.repository.ItemRepository;
import com.echommo.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@Transactional
public class GameService {

    @Autowired private UserRepository userRepo;
    @Autowired private CharacterRepository charRepo;
    @Autowired private ItemRepository itemRepo;

    // Chi phí và cấu hình game
    private static final int EXPLORE_ENERGY_COST = 2;
    private static final int ATTACK_ENERGY_COST = 2;
    private static final int STRONG_ATTACK_ENERGY_COST = 5;
    private static final long BASE_EXP_REQUIREMENT = 100L;


    /**
     * Lấy User/Player và đảm bảo Entity Character đã được khởi tạo
     */
    public User getPlayerOrCreate(Integer userId) {
        return userRepo.findById(userId).map(user -> {
            if (user.getCharacter() == null) {
                Character newChar = new Character();
                newChar.setUser(user);
                user.setCharacter(newChar);
                userRepo.save(user); // Lưu lại User để cập nhật Character
            }
            return user;
        }).orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
    }

    /**
     * Helper: Lấy Character Stats từ User ID
     */
    private Character getCharacter(Integer userId) {
        // Lấy Character thông qua User (đảm bảo Character tồn tại nhờ getPlayerOrCreate)
        User user = userRepo.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (user.getCharacter() == null) {
            throw new IllegalStateException("Character data missing for user: " + userId);
        }
        return user.getCharacter();
    }

    // --- CÁC HÀM LOGIC GAME ---

    private String checkLevelUp(Character character) {
        long requiredExp = BASE_EXP_REQUIREMENT * (long) Math.pow(character.getLv(), 2);

        if (character.getExp() >= requiredExp) {
            // Cập nhật chỉ số trên Character
            character.setExp((int)(character.getExp() - requiredExp));
            character.setLv(character.getLv() + 1);

            // Tăng chỉ số gốc (Base Stats)
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

        // Logic năng lượng
        int energyCost = "strong".equals(attackType) ? STRONG_ATTACK_ENERGY_COST : ATTACK_ENERGY_COST;
        if (character.getEnergy() < energyCost) {
            result.put("status", "FAIL");
            result.put("message", "Năng lượng không đủ để tấn công.");
            return result;
        }
        character.setEnergy(character.getEnergy() - energyCost);

        // Logic chiến đấu
        int enemyAtk = 15 + character.getLv();
        int expReward = 35;
        double damageMultiplier = "strong".equals(attackType) ? 2.0 : 1.0;

        int damageDealt = (int) (character.getBaseAtk() * (0.9 + Math.random() * 0.2) * damageMultiplier);
        int damageTaken = isParried ? 0 : Math.max(1, enemyAtk - (character.getBaseDef() / 2));

        int currentHp = Math.max(0, character.getHp() - damageTaken);
        character.setHp(currentHp);

        // Logic sau trận chiến
        String message;
        if (currentHp == 0) {
            result.put("status", "DIED");
            message = "💀 Thất bại...";
            character.setHp(character.getMaxHp() / 2);
        } else {
            result.put("status", "ALIVE");
            character.setExp(character.getExp() + expReward);
            message = "Trúng! (+" + expReward + " EXP)";

            // Xử lý Level Up
            String lvUpMsg = checkLevelUp(character);
            if (lvUpMsg != null) {
                message = lvUpMsg;
                result.put("levelUp", true);
            }
        }

        charRepo.save(character); // Lưu Character

        // Trả về kết quả mới
        result.put("message", isParried ? "🛡️ PARRY! " + message : message);
        result.put("damageDealt", damageDealt);
        result.put("damageTaken", damageTaken);

        // Trả về chỉ số EXP và LV (dùng cho Frontend update)
        result.put("playerHp", character.getHp());
        result.put("playerEnergy", character.getEnergy());
        result.put("playerExp", character.getExp());
        result.put("playerLv", character.getLv());
        result.put("nextLevelExp", BASE_EXP_REQUIREMENT * (long) Math.pow(character.getLv(), 2));

        return result;
    }

    public Map<String, Object> explore(Integer userId) {
        Character character = getCharacter(userId);
        Map<String, Object> result = new HashMap<>();

        if (character.getEnergy() < EXPLORE_ENERGY_COST) {
            result.put("message", "Năng lượng không đủ để hành tẩu.");
            result.put("type", "FAIL");
            return result;
        }

        character.setEnergy(character.getEnergy() - EXPLORE_ENERGY_COST);
        int expGain = 10;
        character.setExp(character.getExp() + expGain);

        String lvMsg = checkLevelUp(character);
        boolean isLevelUp = (lvMsg != null);

        double roll = Math.random() * 100;
        String message = "";

        // Logic random events giữ nguyên
        if (roll < 40) {
            result.put("type", "NOTHING");
            message = "Khu rừng yên tĩnh...";
        } else if (roll < 70) {
            int goldGain = (int) (Math.random() * 20) + 10;
            // TODO: Cần cập nhật Wallet Entity thay vì Player/User trực tiếp
            // player.setGold(player.getGold() + goldGain);
            result.put("type", "GOLD");
            message = "Nhặt được " + goldGain + " vàng!";
        } else {
            result.put("type", "ENEMY");
            message = "Quái vật xuất hiện!";
        }

        message += " (+" + expGain + " EXP)";
        if(isLevelUp) message += " [LÊN CẤP!]";

        charRepo.save(character); // Lưu Character

        // Trả về kết quả mới
        result.put("message", message);
        result.put("playerExp", character.getExp());
        result.put("playerLv", character.getLv());
        result.put("playerEnergy", character.getEnergy());
        result.put("nextLevelExp", BASE_EXP_REQUIREMENT * (long) Math.pow(character.getLv(), 2));

        return result;
    }

    public User restAtInn(Integer userId) {
        Character character = getCharacter(userId);
        character.setHp(character.getMaxHp());
        character.setEnergy(character.getMaxEnergy());
        charRepo.save(character);
        return character.getUser();
    }

    // --- LOGIC INVENTORY ---

    public List<Item> getInventory(Integer userId) {
        return itemRepo.findByUser_UserId(userId);
    }

    public Map<String, Object> equipItem(Integer userId, Integer itemId) {
        Character character = getCharacter(userId);
        Item itemToEquip = itemRepo.findById(itemId).orElseThrow();
        Map<String, Object> result = new HashMap<>();

        // Kiểm tra chủ sở hữu
        if (!itemToEquip.getUser().getUserId().equals(userId)) {
            result.put("success", false);
            return result;
        }

        // Logic gỡ item cũ cùng loại
        Item currentItem = itemRepo.findByUser_UserIdAndTypeAndIsEquippedTrue(userId, itemToEquip.getType());
        if (currentItem != null) {
            currentItem.setIsEquipped(false);
            // Trừ chỉ số gốc trên Character
            character.setBaseAtk(character.getBaseAtk() - currentItem.getAtkBonus());
            character.setBaseDef(character.getBaseDef() - currentItem.getDefBonus());
            character.setMaxHp(character.getMaxHp() - currentItem.getHpBonus());
            character.setMaxEnergy(character.getMaxEnergy() - currentItem.getEnergyBonus());
            itemRepo.save(currentItem);
        }

        // Mặc item mới
        itemToEquip.setIsEquipped(true);
        // Cộng chỉ số
        character.setBaseAtk(character.getBaseAtk() + itemToEquip.getAtkBonus());
        character.setBaseDef(character.getBaseDef() + itemToEquip.getDefBonus());
        character.setMaxHp(character.getMaxHp() + itemToEquip.getHpBonus());
        character.setMaxEnergy(character.getMaxEnergy() + itemToEquip.getEnergyBonus());

        // Đảm bảo HP/Energy không vượt quá Max mới sau khi mặc đồ
        if (character.getHp() > character.getMaxHp()) character.setHp(character.getMaxHp());
        if (character.getEnergy() > character.getMaxEnergy()) character.setEnergy(character.getMaxEnergy());

        itemRepo.save(itemToEquip);
        charRepo.save(character); // Lưu Character

        result.put("success", true);
        result.put("character", character); // Trả về stats mới
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

        // Gỡ đồ và trừ chỉ số
        item.setIsEquipped(false);

        character.setBaseAtk(character.getBaseAtk() - item.getAtkBonus());
        character.setBaseDef(character.getBaseDef() - item.getDefBonus());
        character.setMaxHp(character.getMaxHp() - item.getHpBonus());
        character.setMaxEnergy(character.getMaxEnergy() - item.getEnergyBonus());

        // Đảm bảo HP/Energy hiện tại không lớn hơn Max mới
        if (character.getHp() > character.getMaxHp()) character.setHp(character.getMaxHp());
        if (character.getEnergy() > character.getMaxEnergy()) character.setEnergy(character.getMaxEnergy());

        itemRepo.save(item);
        charRepo.save(character);

        result.put("success", true);
        result.put("character", character);
        return result;
    }
}