package com.echommo.service;

import com.echommo.dto.BattleResult;
import com.echommo.dto.SubStatDTO;
import com.echommo.entity.*;
import com.echommo.entity.Character;
import com.echommo.enums.Rarity;
import com.echommo.enums.SlotType;
import com.echommo.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class BattleService {

    @Autowired private CharacterRepository charRepo;
    @Autowired private EnemyRepository enemyRepo;
    @Autowired private WalletRepository walletRepo;
    @Autowired private ItemRepository itemRepo;
    @Autowired private UserItemRepository userItemRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private BattleSessionRepository sessionRepo;
    @Autowired private SkillRepository skillRepo;
    @Autowired private ItemGenerationService itemGenService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();
    private static final double DROP_RATE = 0.5; // 50% rớt đồ

    public List<Skill> getAllSkills() {
        return skillRepo.findAll();
    }

    // --- 1. START BATTLE ---
    @Transactional
    public BattleResult startBattle() {
        User user = getCurrentUser();
        Character character = charRepo.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Chưa tạo nhân vật"));

        BattleSession session = sessionRepo.findByUser_UserId(user.getUserId())
                .orElse(new BattleSession());

        // Reset hoặc tạo mới session
        session.setUser(user);

        // Random Enemy
        List<Enemy> enemies = enemyRepo.findAll();
        Enemy enemy;
        if (enemies.isEmpty()) {
            // Dummy enemy nếu DB rỗng
            enemy = new Enemy(); enemy.setEnemyId(0); enemy.setName("Bù Nhìn");
            enemy.setHp(100); enemy.setAtk(5); enemy.setDef(0); enemy.setSpeed(10);
        } else {
            enemy = enemies.get(random.nextInt(enemies.size()));
        }

        // Setup Enemy Stats
        session.setEnemyId(enemy.getEnemyId());
        session.setEnemyName(enemy.getName());
        session.setEnemyMaxHp(enemy.getHp());
        session.setEnemyCurrentHp(enemy.getHp());
        session.setEnemyAtk(enemy.getAtk());
        session.setEnemyDef(enemy.getDef());
        session.setEnemySpeed(enemy.getSpeed());

        // Setup Player Stats
        int[] bonusStats = calculatePlayerStats(character);
        // bonusStats: [0]=ATK, [1]=DEF, [2]=CRIT, [3]=HP, [4]=SPEED, [5]=CRIT_DMG

        session.setPlayerMaxHp(character.getMaxHp() + bonusStats[3]);
        session.setPlayerCurrentHp(session.getPlayerMaxHp());
        session.setPlayerCurrentEnergy(character.getCurrentEnergy());

        session.setCurrentTurn(0);
        session.setQteActive(false);
        session.setQteExpiryTime(null);

        return buildResult(sessionRepo.save(session), "Gặp " + enemy.getName() + "! (HP: " + session.getEnemyMaxHp() + ")", "ONGOING");
    }

    // --- 2. PROCESS TURN ---
    @Transactional
    public BattleResult processTurn(String actionType) {
        User user = getCurrentUser();
        BattleSession session = sessionRepo.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Chưa vào trận!"));

        Character character = charRepo.findByUser_UserId(user.getUserId()).get();
        List<String> logs = new ArrayList<>();

        if (session.getEnemyCurrentHp() <= 0) return handleWin(session, character);
        if (session.getPlayerCurrentHp() <= 0) return handleLoss(session);

        // A. Xử lý QTE (Quick Time Event)
        if (session.isQteActive()) {
            // Check hết hạn QTE
            if (session.getQteExpiryTime() != null && LocalDateTime.now().isAfter(session.getQteExpiryTime())) {
                logs.add("⏳ Quá trễ! Bạn không kịp đỡ đòn.");
                actionType = "MISS"; // Ép buộc nhận dam
            }

            int[] stats = calculatePlayerStats(character);
            if ("BLOCK".equals(actionType)) {
                logs.add("🛡️ ĐỠ ĐÒN THÀNH CÔNG! (0 sát thương)");
            } else {
                // Đỡ trượt hoặc hết giờ
                int def = character.getBaseDef() + stats[1];
                int dmg = Math.max(1, session.getEnemyAtk() - def); // Ít nhất 1 damage
                session.setPlayerCurrentHp(session.getPlayerCurrentHp() - dmg);
                logs.add("❌ Bị đánh trúng! Mất " + dmg + " máu.");

                if (session.getPlayerCurrentHp() <= 0) return handleLoss(session);
            }
            // Reset QTE
            session.setQteActive(false);
            session.setQteExpiryTime(null);
            sessionRepo.save(session);
            return buildResult(session, logs, "ONGOING");
        }

        // B. TURN LOGIC (Đánh thường)
        session.setCurrentTurn(session.getCurrentTurn() + 1);
        int[] stats = calculatePlayerStats(character);

        // --- 1. Player Attack ---
        int pAtk = character.getBaseAtk() + stats[0];
        int pCritRate = character.getBaseCritRate() + stats[2]; // Flat point

        int pCritDmgPercent = character.getBaseCritDmg() + stats[5]; // VD: 150 + 50 = 200%

        boolean isCrit = random.nextInt(1000) < pCritRate; // Rate tính theo scale 1000

        int rawDmg = Math.max(1, pAtk - session.getEnemyDef());
        int finalDmg = rawDmg;

        if (isCrit) {
            finalDmg = (int) (rawDmg * (pCritDmgPercent / 100.0));
            logs.add("💥 BẠO KÍCH! Gây " + finalDmg + " sát thương!");
        } else {
            logs.add("⚔️ Tấn công gây " + finalDmg + " sát thương.");
        }

        session.setEnemyCurrentHp(session.getEnemyCurrentHp() - finalDmg);

        if (session.getEnemyCurrentHp() <= 0) {
            sessionRepo.save(session);
            return handleWin(session, character);
        }

        // --- 2. Enemy Attack ---
        // 30% tỉ lệ kích hoạt QTE
        if (random.nextInt(100) < 30) {
            session.setQteActive(true);
            session.setQteExpiryTime(LocalDateTime.now().plusSeconds(3)); // 3 giây để bấm
            logs.add("⚠️ " + session.getEnemyName() + " chuẩn bị tung chiêu mạnh! ĐỠ NGAY (3s)!");
            sessionRepo.save(session);

            BattleResult res = buildResult(session, logs, "QTE_ACTION");
            res.setQteTriggered(true);
            return res;
        }

        // Đánh thường trả đòn
        int pDef = character.getBaseDef() + stats[1];
        int dmgToPlayer = Math.max(1, session.getEnemyAtk() - pDef);
        session.setPlayerCurrentHp(session.getPlayerCurrentHp() - dmgToPlayer);
        logs.add("👾 " + session.getEnemyName() + " đánh trả " + dmgToPlayer + " máu.");

        if (session.getPlayerCurrentHp() <= 0) return handleLoss(session);

        sessionRepo.save(session);
        return buildResult(session, logs, "ONGOING");
    }

    // --- 3. TÍNH CHỈ SỐ (FIXED LOGIC) ---
    // Return: [ATK, DEF, CRIT_RATE, HP, SPEED, CRIT_DMG] (Bonus values only)
    private int[] calculatePlayerStats(Character c) {
        double[] flatStats = new double[6];
        double[] percentStats = new double[6];

        List<UserItem> items = userItemRepo.findByUser_UserIdAndIsEquippedTrue(c.getUser().getUserId());

        for (UserItem ui : items) {
            // 1. Main Stat
            if (ui.getMainStatType() != null) {
                parseStatToArrays(ui.getMainStatType(), ui.getMainStatValue().doubleValue(), flatStats, percentStats);
            }
            // 2. Sub Stats
            if (ui.getSubStats() != null && !ui.getSubStats().isEmpty()) {
                try {
                    List<SubStatDTO> subs = objectMapper.readValue(ui.getSubStats(), new TypeReference<List<SubStatDTO>>() {});
                    for (SubStatDTO sub : subs) {
                        // [FIX QUAN TRỌNG] Đổi getType() thành getCode()
                        parseStatToArrays(sub.getCode(), sub.getValue(), flatStats, percentStats);
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi parse stats item " + ui.getUserItemId());
                }
            }
        }

        // 3. Tổng hợp lại: Bonus = Flat + (Base + Flat) * Percent
        int[] finalBonus = new int[6];

        // ATK
        double totalAtk = flatStats[0] + ((c.getBaseAtk() + flatStats[0]) * percentStats[0] / 100.0);
        finalBonus[0] = (int) totalAtk;

        // DEF
        double totalDef = flatStats[1] + ((c.getBaseDef() + flatStats[1]) * percentStats[1] / 100.0);
        finalBonus[1] = (int) totalDef;

        // CRIT RATE
        finalBonus[2] = (int) (flatStats[2] + percentStats[2]);

        // HP
        double totalHp = flatStats[3] + ((c.getMaxHp() + flatStats[3]) * percentStats[3] / 100.0);
        finalBonus[3] = (int) totalHp;

        // SPEED
        finalBonus[4] = (int) (flatStats[4] + percentStats[4]);

        // CRIT DMG
        finalBonus[5] = (int) (flatStats[5] + percentStats[5]);

        return finalBonus;
    }

    private void parseStatToArrays(String type, double val, double[] flats, double[] percents) {
        switch (type) {
            case "ATK_FLAT" -> flats[0] += val;
            case "ATK_PERCENT" -> percents[0] += val;

            case "DEF_FLAT" -> flats[1] += val;
            case "DEF_PERCENT" -> percents[1] += val;

            case "CRIT_RATE" -> flats[2] += val;
            case "CRIT_RATE_PERCENT" -> percents[2] += val;

            case "HP_FLAT" -> flats[3] += val;
            case "HP_PERCENT" -> percents[3] += val;

            case "SPEED" -> flats[4] += val;
            case "CRIT_DMG" -> flats[5] += val;
            case "CRIT_DMG_PERCENT" -> percents[5] += val;
        }
    }


    // --- 4. RỚT ĐỒ ---
    private void handleNewItemDrop(User user, List<String> logs, BattleResult result) {
        if (random.nextDouble() > DROP_RATE) return;

        List<Item> allItems = itemRepo.findAll();
        if (allItems.isEmpty()) return;

        Item baseItem = allItems.get(random.nextInt(allItems.size()));

        UserItem newItem = new UserItem();
        newItem.setUser(user);
        newItem.setItem(baseItem);
        newItem.setQuantity(1);
        newItem.setIsEquipped(false);
        newItem.setEnhanceLevel(0);
        newItem.setAcquiredAt(LocalDateTime.now());

        // RNG Rarity
        int roll = random.nextInt(100);
        Rarity rarity;
        if (roll < 50) rarity = Rarity.COMMON;
        else if (roll < 80) rarity = Rarity.RARE;
        else if (roll < 95) rarity = Rarity.EPIC;
        else rarity = Rarity.LEGENDARY;
        newItem.setRarity(rarity);

        // Main Stat theo Slot
        String mainStatType = "ATK_FLAT";
        if (baseItem.getSlotType() == SlotType.WEAPON) mainStatType = "ATK_FLAT";
        else if (baseItem.getSlotType() == SlotType.ARMOR) mainStatType = "DEF_FLAT";
        else if (baseItem.getSlotType() == SlotType.HELMET) mainStatType = "HP_FLAT";
        else if (baseItem.getSlotType() == SlotType.BOOTS) mainStatType = "SPEED";

        newItem.setMainStatType(mainStatType);
        newItem.setMainStatValue(BigDecimal.valueOf(10 * baseItem.getTier()));

        // Generate Substats
        List<SubStatDTO> subStats = new ArrayList<>();

        // [FIX] Cập nhật Switch Case cho Rarity mới (Uncommon/Mythic)
        int lines = switch (rarity) {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 2;
            case EPIC -> 3;
            case LEGENDARY, MYTHIC -> 4;
            default -> 1;
        };

        for (int i = 0; i < lines; i++) {
            subStats.add(itemGenService.generateRandomSubStat(newItem, subStats));
        }

        try {
            newItem.setSubStats(objectMapper.writeValueAsString(subStats));
        } catch (Exception e) {
            newItem.setSubStats("[]");
        }

        userItemRepo.save(newItem);

        logs.add("🎁 NHẶT ĐƯỢC: [" + rarity + "] " + baseItem.getName());
        result.setDroppedItemName(baseItem.getName());
        result.setDroppedItemImage(baseItem.getImageUrl());
        result.setDroppedItemRarity(rarity.name());
    }

    // --- HELPER METHODS ---
    private User getCurrentUser() {
        return userRepo.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private BattleResult handleWin(BattleSession session, Character character) {
        BattleResult res = buildResult(session, "🏆 Chiến thắng!", "VICTORY");
        Enemy enemyRef = enemyRepo.findById(session.getEnemyId()).orElse(null);

        int exp = (enemyRef != null) ? enemyRef.getExpReward() : 10;
        int gold = (enemyRef != null) ? enemyRef.getGoldReward() : 10;

        // Level Up Logic
        character.setCurrentExp(character.getCurrentExp() + exp);
        long requiredExp = character.getLevel() * 100L;
        if (character.getCurrentExp() >= requiredExp) {
            character.setCurrentExp(character.getCurrentExp() - (int)requiredExp);
            character.setLevel(character.getLevel() + 1);
            character.setMaxHp(character.getMaxHp() + 50);
            character.setCurrentHp(character.getMaxHp());
            character.setStatPoints(character.getStatPoints() + 5);
            res.setLevelUp(true);
            logsAdd(res, "🆙 LÊN CẤP " + character.getLevel() + "!");
        }

        // Add Gold
        Wallet w = walletRepo.findByUser_UserId(character.getUser().getUserId()).orElse(null);
        if (w != null) {
            w.setGold(w.getGold().add(BigDecimal.valueOf(gold)));
            walletRepo.save(w);
        }

        character.setCurrentHp(Math.max(1, session.getPlayerCurrentHp()));
        charRepo.save(character);

        res.setExpEarned(exp);
        res.setGoldEarned(gold);

        handleNewItemDrop(character.getUser(), res.getCombatLog(), res);

        sessionRepo.delete(session);
        return res;
    }

    private BattleResult handleLoss(BattleSession session) {
        BattleResult res = buildResult(session, "💀 Thất bại... Bạn đã ngất xỉu.", "DEFEAT");
        Character c = charRepo.findByUser_UserId(session.getUser().getUserId()).get();
        c.setCurrentHp(1); // Hồi sinh tại chỗ với 1 máu
        charRepo.save(c);
        sessionRepo.delete(session);
        return res;
    }

    private BattleResult buildResult(BattleSession s, List<String> logs, String status) {
        BattleResult res = new BattleResult();
        res.setEnemyId(s.getEnemyId()); res.setEnemyName(s.getEnemyName());
        res.setEnemyHp(s.getEnemyCurrentHp()); res.setEnemyMaxHp(s.getEnemyMaxHp());
        res.setPlayerHp(s.getPlayerCurrentHp()); res.setPlayerMaxHp(s.getPlayerMaxHp());
        res.setPlayerEnergy(s.getPlayerCurrentEnergy()); res.setCombatLog(logs); res.setStatus(status);
        return res;
    }

    private BattleResult buildResult(BattleSession s, String msg, String status) {
        List<String> logs = new ArrayList<>(); logs.add(msg); return buildResult(s, logs, status);
    }

    private void logsAdd(BattleResult res, String msg) {
        List<String> logs = res.getCombatLog();
        if(logs == null) logs = new ArrayList<>();
        logs.add(msg);
        res.setCombatLog(logs);
    }

    @Transactional
    public BattleResult attackEnemy(Map<String, Object> payload) {
        return processTurn("ATTACK");
    }
}