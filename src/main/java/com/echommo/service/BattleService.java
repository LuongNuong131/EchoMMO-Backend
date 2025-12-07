package com.echommo.service;

import com.echommo.dto.BattleResult;
import com.echommo.entity.*;
import com.echommo.entity.Character;
import com.echommo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class BattleService {

    @Autowired private CharacterRepository charRepo;
    @Autowired private EnemyRepository enemyRepo;
    @Autowired private WalletRepository walletRepo;
    @Autowired private ItemRepository itemRepo;
    @Autowired private UserItemRepository userItemRepo;
    @Autowired private UserRepository userRepo; // Inject thêm để lấy User từ Token

    private final Random random = new Random();
    private static final double DROP_RATE = 0.3;

    public List<Skill> getAllSkills() {
        return new ArrayList<>();
    }

    // [FIX] Hàm này giờ sẽ random quái vật để trả về cho Frontend hiển thị
    @Transactional
    public BattleResult startBattle() {
        // Lấy danh sách quái (hoặc logic tìm theo level)
        List<Enemy> enemies = enemyRepo.findAll();
        if (enemies.isEmpty()) {
            // Tạo quái dummy nếu DB chưa có
            Enemy dummy = new Enemy();
            dummy.setEnemyId(1);
            dummy.setName("Hình Nhân Gỗ");
            dummy.setHp(100);
            dummy.setLevel(1);
            dummy.setAtk(5);
            dummy.setDef(0);
            dummy.setExpReward(10);
            dummy.setGoldReward(10);

            BattleResult res = new BattleResult();
            res.setEnemy(dummy);
            res.setEnemyHp(100);
            res.setEnemyMaxHp(100);
            res.setStatus("ONGOING");
            return res;
        }

        // Random 1 con quái
        Enemy enemy = enemies.get(random.nextInt(enemies.size()));

        BattleResult result = new BattleResult();
        result.setEnemy(enemy);
        result.setEnemyHp(enemy.getHp());
        result.setEnemyMaxHp(enemy.getHp());

        // Lấy thông tin nhân vật để hiện HP ban đầu (Optional)
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByUsername(username).orElse(null);
        if (user != null) {
            Character c = charRepo.findByUser_UserId(user.getUserId()).orElse(null);
            if (c != null) {
                result.setPlayerHp(c.getHp());
                result.setPlayerMaxHp(c.getMaxHp());
                result.setPlayerEnergy(c.getEnergy());
            }
        }

        result.setStatus("ONGOING");
        result.setMessage("Gặp " + enemy.getName() + "! Chuẩn bị chiến đấu.");
        return result;
    }

    // [FIX] Tự động lấy charId từ Token, không bắt Frontend gửi lên nữa
    @Transactional
    public BattleResult attackEnemy(Map<String, Object> payload) {
        if (!payload.containsKey("enemyId")) {
            throw new RuntimeException("Thiếu enemyId");
        }

        // Parse ID từ payload (phòng trường hợp gửi chuỗi)
        Integer enemyId = Integer.parseInt(payload.get("enemyId").toString());

        // Lấy User đang đăng nhập
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Lấy Character của User đó
        Character character = charRepo.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Chưa tạo nhân vật"));

        // Gọi hàm fight gốc
        return fight(character.getCharId(), enemyId);
    }

    @Transactional
    public BattleResult fight(Integer charId, Integer enemyId) {
        Character character = charRepo.findById(charId)
                .orElseThrow(() -> new RuntimeException("Character not found"));
        Enemy enemy = enemyRepo.findById(enemyId)
                .orElseThrow(() -> new RuntimeException("Enemy not found"));

        List<String> logs = new ArrayList<>();

        // 1. TÍNH CHỈ SỐ TỔNG
        List<UserItem> equippedItems = userItemRepo.findByUser_UserIdAndIsEquippedTrue(character.getUser().getUserId());

        int totalAtk = character.getBaseAtk();
        int totalDef = character.getBaseDef();
        int totalCrit = character.getBaseCritRate();
        int extraHp = 0;

        for (UserItem ui : equippedItems) {
            totalAtk += ui.getItem().getAtkBonus();
            totalDef += ui.getItem().getDefBonus();
            totalCrit += ui.getItem().getCritRateBonus();
            extraHp += ui.getItem().getHpBonus();
        }

        int maxTotalHp = character.getMaxHp() + extraHp;
        // Vào trận hồi đầy máu (hoặc dùng máu hiện tại tùy game design)
        int currentHp = maxTotalHp;

        // Lấy HP quái từ client gửi lên nếu có (để đánh tiếp) nhưng ở đây ta làm đơn giản là đánh 1 turn
        // Trong logic Turn-based web, mỗi lần gọi API là 1 turn hoặc 1 chuỗi turn.
        // Code gốc của bạn là Auto-battle (đánh tới chết trong 1 lần gọi)

        int enemyHp = enemy.getHp();

        logs.add("⚔️ Bắt đầu: " + character.getName() + " vs " + enemy.getName());

        // 2. BATTLE LOOP (Đánh tới khi 1 bên chết)
        int turn = 0;
        boolean isWin = false;

        while (currentHp > 0 && enemyHp > 0 && turn < 50) {
            turn++;

            // --- Player Turn ---
            int dmgToEnemy = Math.max(1, totalAtk - enemy.getDef());
            if (random.nextInt(100) < totalCrit) {
                dmgToEnemy = (int) (dmgToEnemy * (character.getBaseCritDmg() / 100.0));
                logs.add("[Turn " + turn + "] 💥 BẠO KÍCH! Bạn gây " + dmgToEnemy + " sát thương!");
            } else {
                logs.add("[Turn " + turn + "] 🗡️ Bạn gây " + dmgToEnemy + " sát thương.");
            }
            enemyHp -= dmgToEnemy;

            if (enemyHp <= 0) {
                isWin = true;
                break;
            }

            // --- Enemy Turn ---
            int dmgToChar = Math.max(1, enemy.getAtk() - totalDef);
            currentHp -= dmgToChar;
            logs.add("[Turn " + turn + "] 👾 " + enemy.getName() + " đánh lại " + dmgToChar + " sát thương!");
        }

        // 3. KẾT QUẢ
        BattleResult result = new BattleResult();
        result.setEnemy(enemy);
        result.setPlayerHp(Math.max(0, currentHp));
        result.setPlayerMaxHp(maxTotalHp);
        result.setPlayerEnergy(character.getEnergy());
        result.setEnemyHp(Math.max(0, enemyHp));
        result.setEnemyMaxHp(enemy.getHp());
        result.setCombatLog(logs);
        result.setStatus(isWin ? "VICTORY" : "DEFEAT");

        if (isWin) {
            int exp = enemy.getExpReward();
            int gold = enemy.getGoldReward();

            character.setExp(character.getExp() + exp);
            long reqExp = character.getLv() * 100L;

            if (character.getExp() >= reqExp) {
                character.setExp((int)(character.getExp() - reqExp));
                character.setLv(character.getLv() + 1);
                character.setBaseAtk(character.getBaseAtk() + 5);
                character.setBaseDef(character.getBaseDef() + 2);
                character.setMaxHp(character.getMaxHp() + 50);
                character.setHp(character.getMaxHp());
                logs.add("🌟 CHÚC MỪNG! Bạn đã thăng cấp " + character.getLv());
                result.setLevelUp(true);
            } else {
                character.setHp(Math.min(currentHp, character.getMaxHp()));
            }

            Wallet wallet = walletRepo.findByUser_UserId(character.getUser().getUserId())
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));
            wallet.setGold(wallet.getGold().add(new BigDecimal(gold)));
            walletRepo.save(wallet);

            logs.add("🏆 Chiến thắng! Nhận được " + exp + " EXP và " + gold + " Vàng.");
            result.setExpEarned(exp);
            result.setGoldEarned(gold);

            handleItemDrop(character.getUser(), logs, result);

        } else {
            logs.add("💀 Bạn đã thất bại... Hãy luyện tập thêm.");
            character.setHp(1); // Hồi 1 máu để không chết hẳn
        }

        charRepo.save(character);
        return result;
    }

    private void handleItemDrop(User user, List<String> logs, BattleResult result) {
        if (random.nextDouble() <= DROP_RATE) {
            List<String> equipmentTypes = Arrays.asList("WEAPON", "ARMOR", "HELMET", "BOOTS", "RING", "NECKLACE");

            List<Item> droppableItems = new ArrayList<>();
            for (String type : equipmentTypes) {
                droppableItems.addAll(itemRepo.findByType(type));
            }

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