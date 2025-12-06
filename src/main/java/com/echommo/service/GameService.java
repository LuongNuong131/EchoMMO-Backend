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
            character.setBaseAtk(character.getBaseAtk() + 5);
            character.setBaseDef(character.getBaseDef() + 2);
            character.setMaxHp(character.getMaxHp() + 20);
            character.setMaxEnergy(character.getMaxEnergy() + 5);
            character.setHp(character.getMaxHp());
            character.setEnergy(character.getMaxEnergy());
            charRepo.save(character);
            return "LEVEL UP! Cấp " + character.getLv();
        }
        return null;
    }

    // [FIX] HÀNH TẨU MIỄN PHÍ (Không trừ Energy)
    public Map<String, Object> explore(Integer userId) {
        Character character = getCharacter(userId);
        Map<String, Object> result = new HashMap<>();

        // Cộng EXP
        int expGain = 10;
        character.setExp(character.getExp() + expGain);
        String lvMsg = checkLevelUp(character);
        boolean isLevelUp = (lvMsg != null);

        // Random sự kiện
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

    // Nghỉ ngơi tốn 50 vàng
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