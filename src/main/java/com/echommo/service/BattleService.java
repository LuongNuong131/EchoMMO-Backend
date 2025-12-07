package com.echommo.service;

import com.echommo.dto.BattleResult;
import com.echommo.entity.*;
import com.echommo.entity.Character;
import com.echommo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
public class BattleService {

    @Autowired private CharacterRepository charRepo;
    @Autowired private EnemyRepository enemyRepo;
    @Autowired private WalletRepository walletRepo;
    @Autowired private ItemRepository itemRepo;
    @Autowired private UserItemRepository userItemRepo; // Dùng để lấy đồ đang mặc

    private final Random random = new Random();
    private static final double DROP_RATE = 0.3;

    @Transactional
    public BattleResult fight(Long charId, Long enemyId) {
        Character character = charRepo.findById(charId)
                .orElseThrow(() -> new RuntimeException("Character not found"));
        Enemy enemy = enemyRepo.findById(enemyId)
                .orElseThrow(() -> new RuntimeException("Enemy not found"));

        List<String> logs = new ArrayList<>();

        // 1. TÍNH CHỈ SỐ TỔNG (Base + Trang bị)
        List<UserItem> equippedItems = userItemRepo.findByUser_UserIdAndIsEquippedTrue(character.getUser().getUserId());

        int totalAtk = character.getBaseAtk();
        int totalDef = character.getBaseDef();
        int totalCrit = character.getBaseCritRate();
        int extraHp = 0; // Máu cộng thêm từ đồ

        for (UserItem ui : equippedItems) {
            totalAtk += ui.getItem().getAtkBonus();
            totalDef += ui.getItem().getDefBonus();
            totalCrit += ui.getItem().getCritRateBonus();
            extraHp += ui.getItem().getHpBonus();
        }

        // HP dùng trong trận = HP hiện tại (không vượt quá Max Base + Max Item)
        int maxTotalHp = character.getMaxHp() + extraHp;
        // Logic đơn giản: Khi vào trận hồi đầy máu (hoặc dùng máu hiện tại tùy game design)
        // Ở đây giả sử hồi đầy cho dễ test
        int currentHp = maxTotalHp;
        int enemyHp = enemy.getHp();

        logs.add("⚔️ Bắt đầu trận đấu! (Công lực: " + totalAtk + " | Hộ thể: " + totalDef + ")");

        // 2. BATTLE LOOP
        int turn = 0;
        boolean isWin = false;

        while (currentHp > 0 && enemyHp > 0 && turn < 50) {
            turn++;

            // --- Player Turn ---
            int dmgToEnemy = Math.max(1, totalAtk - enemy.getDef());
            // Crit?
            if (random.nextInt(100) < totalCrit) {
                dmgToEnemy = (int) (dmgToEnemy * (character.getBaseCritDmg() / 100.0));
                logs.add("[Turn " + turn + "] 💥 Bạn bạo kích: " + dmgToEnemy + " dmg!");
            } else {
                logs.add("[Turn " + turn + "] 🗡️ Bạn tấn công: " + dmgToEnemy + " dmg.");
            }
            enemyHp -= dmgToEnemy;

            if (enemyHp <= 0) {
                isWin = true;
                break;
            }

            // --- Enemy Turn ---
            int dmgToChar = Math.max(1, enemy.getAtk() - totalDef);
            currentHp -= dmgToChar;
            logs.add("[Turn " + turn + "] 👾 " + enemy.getName() + " đánh lại: " + dmgToChar + " dmg!");
        }

        // 3. KẾT QUẢ
        BattleResult result = new BattleResult();
        result.setLogs(logs);
        result.setWin(isWin);

        if (isWin) {
            // Logic nhận thưởng giữ nguyên như cũ
            int exp = enemy.getExpReward();
            int gold = enemy.getGoldReward();

            character.setExp(character.getExp() + exp);
            if (character.getExp() >= character.getLv() * 100L) {
                character.setExp(character.getExp() - (character.getLv() * 100L));
                character.setLv(character.getLv() + 1);
                character.setBaseAtk(character.getBaseAtk() + 10);
                character.setBaseDef(character.getBaseDef() + 5);
                character.setMaxHp(character.getMaxHp() + 100);
                character.setHp(character.getMaxHp());
                logs.add("🌟 LEVEL UP! Cấp " + character.getLv());
            } else {
                // Cập nhật máu sau trận (không quá Max gốc, xử lý đơn giản)
                character.setHp(Math.min(currentHp, character.getMaxHp()));
            }

            Wallet wallet = walletRepo.findByUser_UserId(character.getUser().getUserId())
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));
            wallet.setGold(wallet.getGold().add(new BigDecimal(gold)));
            walletRepo.save(wallet);

            logs.add("🏆 Chiến thắng! Nhận " + exp + " EXP, " + gold + " vàng.");
            result.setExpEarned(exp);
            result.setGoldEarned(gold);

            handleItemDrop(character.getUser(), logs, result);

        } else {
            logs.add("💀 Thất bại...");
            character.setHp(1);
        }

        charRepo.save(character);
        return result;
    }

    private void handleItemDrop(User user, List<String> logs, BattleResult result) {
        if (random.nextDouble() <= DROP_RATE) {
            List<String> equipmentTypes = Arrays.asList("WEAPON", "ARMOR", "HELMET", "BOOTS", "RING", "NECKLACE");
            List<Item> droppableItems = itemRepo.findByTypeIn(equipmentTypes);

            if (!droppableItems.isEmpty()) {
                Item droppedItem = droppableItems.get(random.nextInt(droppableItems.size()));
                UserItem newItem = new UserItem();
                newItem.setUser(user);
                newItem.setItem(droppedItem);
                newItem.setQuantity(1);
                newItem.setIsEquipped(false);
                newItem.setEnhanceLevel(0);
                userItemRepo.save(newItem);

                logs.add("🎁 NHẶT ĐƯỢC: [" + droppedItem.getName() + "]");
                result.setDroppedItemName(droppedItem.getName());
                result.setDroppedItemImage(droppedItem.getImageUrl());
                result.setDroppedItemRarity(droppedItem.getRarity());
            }
        }
    }
}