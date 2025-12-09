package com.echommo.service;

import com.echommo.dto.BattleResult;
import com.echommo.entity.*;
import com.echommo.entity.Character;
import com.echommo.enums.Rarity;
import com.echommo.enums.SlotType;
import com.echommo.repository.*;
import com.echommo.service.ItemGenerationService.SubStatDTO;
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

    // [FIX] Thêm Repository này để lấy danh sách Skill
    @Autowired private SkillRepository skillRepo;

    @Autowired private ItemGenerationService itemGenService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Random random = new Random();
    private static final double DROP_RATE = 0.5;

    // --- 0. [FIX] HÀM BỊ THIẾU ---
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

        if (session.getId() == null) {
            session.setUser(user);
        }

        List<Enemy> enemies = enemyRepo.findAll();
        Enemy enemy;
        if (enemies.isEmpty()) {
            enemy = new Enemy(); enemy.setEnemyId(0); enemy.setName("Bù Nhìn");
            enemy.setHp(100); enemy.setAtk(5); enemy.setDef(0);
        } else {
            enemy = enemies.get(random.nextInt(enemies.size()));
        }

        session.setEnemyId(enemy.getEnemyId());
        session.setEnemyName(enemy.getName());
        session.setEnemyMaxHp(enemy.getHp());
        session.setEnemyCurrentHp(enemy.getHp());
        session.setEnemyAtk(enemy.getAtk());
        session.setEnemyDef(enemy.getDef());
        session.setEnemySpeed(enemy.getSpeed());

        // Tính stats chuẩn từ đồ đạc
        int[] stats = calculatePlayerStats(character);
        // stats[0]=ATK, [1]=DEF, [2]=CRIT, [3]=HP, [4]=SPEED, [5]=CRIT_DMG

        session.setPlayerMaxHp(character.getMaxHp() + stats[3]);
        session.setPlayerCurrentHp(session.getPlayerMaxHp());
        session.setPlayerCurrentEnergy(character.getCurrentEnergy());
        session.setCurrentTurn(0);
        session.setQteActive(false);

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

        // A. Xử lý QTE
        if (session.isQteActive()) {
            int[] stats = calculatePlayerStats(character);
            if ("BLOCK".equals(actionType)) {
                logs.add("🛡️ ĐỠ ĐÒN THÀNH CÔNG! (0 sát thương)");
                session.setQteActive(false);
                sessionRepo.save(session);
                return buildResult(session, logs, "ONGOING");
            } else {
                int def = character.getBaseDef() + stats[1];
                int dmg = Math.max(1, session.getEnemyAtk() - def);
                session.setPlayerCurrentHp(session.getPlayerCurrentHp() - dmg);
                logs.add("❌ Đỡ trượt! Bị đánh " + dmg + " máu.");
                session.setQteActive(false);

                if (session.getPlayerCurrentHp() <= 0) return handleLoss(session);
                sessionRepo.save(session);
                return buildResult(session, logs, "ONGOING");
            }
        }

        // B. TURN LOGIC
        session.setCurrentTurn(session.getCurrentTurn() + 1);
        int[] stats = calculatePlayerStats(character);

        // --- Player Attack ---
        int pAtk = character.getBaseAtk() + stats[0];
        int pCritRate = character.getBaseCritRate() + stats[2];
        int pCritDmg = character.getBaseCritDmg() + stats[5];

        boolean isCrit = random.nextInt(1000) < pCritRate;

        int rawDmg = Math.max(1, pAtk - session.getEnemyDef());
        int finalDmg = rawDmg;

        if (isCrit) {
            finalDmg = (int) (rawDmg * (pCritDmg / 100.0));
            logs.add("💥 BẠO KÍCH! Gây " + finalDmg + " sát thương!");
        } else {
            logs.add("⚔️ Tấn công gây " + finalDmg + " sát thương.");
        }

        session.setEnemyCurrentHp(session.getEnemyCurrentHp() - finalDmg);

        if (session.getEnemyCurrentHp() <= 0) {
            sessionRepo.save(session);
            return handleWin(session, character);
        }

        // --- Enemy Attack ---
        if (random.nextInt(100) < 30) {
            session.setQteActive(true);
            session.setQteExpiryTime(LocalDateTime.now().plusSeconds(3));
            logs.add("⚠️ " + session.getEnemyName() + " chuẩn bị tung chiêu mạnh! ĐỠ NGAY!");
            sessionRepo.save(session);
            BattleResult res = buildResult(session, logs, "QTE_ACTION");
            res.setQteTriggered(true);
            return res;
        }

        int pDef = character.getBaseDef() + stats[1];
        int dmgToPlayer = Math.max(1, session.getEnemyAtk() - pDef);
        session.setPlayerCurrentHp(session.getPlayerCurrentHp() - dmgToPlayer);
        logs.add("👾 " + session.getEnemyName() + " đánh trả " + dmgToPlayer + " máu.");

        if (session.getPlayerCurrentHp() <= 0) return handleLoss(session);

        sessionRepo.save(session);
        return buildResult(session, logs, "ONGOING");
    }

    // --- 3. TÍNH CHỈ SỐ ---
    private int[] calculatePlayerStats(Character c) {
        int[] totalStats = new int[6];
        List<UserItem> items = userItemRepo.findByUser_UserIdAndIsEquippedTrue(c.getUser().getUserId());

        for (UserItem ui : items) {
            if (ui.getMainStatType() != null) {
                addStatValue(totalStats, ui.getMainStatType(), ui.getMainStatValue().doubleValue());
            }
            if (ui.getSubStats() != null && !ui.getSubStats().isEmpty()) {
                try {
                    List<SubStatDTO> subs = objectMapper.readValue(ui.getSubStats(), new TypeReference<List<SubStatDTO>>() {});
                    for (SubStatDTO sub : subs) {
                        addStatValue(totalStats, sub.getType(), sub.getValue());
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi parse stats item " + ui.getUserItemId());
                }
            }
        }
        return totalStats;
    }

    private void addStatValue(int[] stats, String type, double value) {
        int val = (int) value;
        switch (type) {
            case "ATK_FLAT": stats[0] += val; break;
            case "ATK_PERCENT": stats[0] += (stats[0] * val / 100); break;
            case "DEF_FLAT": stats[1] += val; break;
            case "DEF_PERCENT": stats[1] += (stats[1] * val / 100); break;
            case "CRIT_RATE": stats[2] += val; break;
            case "HP_FLAT": stats[3] += val; break;
            case "HP_PERCENT": stats[3] += (stats[3] * val / 100); break;
            case "SPEED": stats[4] += val; break;
            case "CRIT_DMG": stats[5] += val; break;
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

        int roll = random.nextInt(100);
        Rarity rarity;
        if (roll < 50) rarity = Rarity.COMMON;
        else if (roll < 80) rarity = Rarity.RARE;
        else if (roll < 95) rarity = Rarity.EPIC;
        else rarity = Rarity.LEGENDARY;
        newItem.setRarity(rarity);

        String mainStatType = "ATK_FLAT";
        if (baseItem.getSlotType() == SlotType.WEAPON) mainStatType = "ATK_FLAT";
        else if (baseItem.getSlotType() == SlotType.ARMOR) mainStatType = "DEF_FLAT";
        else if (baseItem.getSlotType() == SlotType.HELMET) mainStatType = "HP_FLAT";
        else if (baseItem.getSlotType() == SlotType.BOOTS) mainStatType = "SPEED";

        newItem.setMainStatType(mainStatType);
        newItem.setMainStatValue(BigDecimal.valueOf(10 * baseItem.getTier()));

        List<SubStatDTO> subStats = new ArrayList<>();
        int lines = switch (rarity) {
            case COMMON -> 1;
            case RARE -> 2;
            case RARE_PLUS -> 2;
            case EPIC -> 3;
            case LEGENDARY -> 4;
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

    private User getCurrentUser() {
        return userRepo.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private BattleResult handleWin(BattleSession session, Character character) {
        BattleResult res = buildResult(session, "🏆 Chiến thắng!", "VICTORY");
        Enemy enemyRef = enemyRepo.findById(session.getEnemyId()).orElse(null);

        int exp = (enemyRef != null) ? enemyRef.getExpReward() : 10;
        int gold = (enemyRef != null) ? enemyRef.getGoldReward() : 10;

        character.setCurrentExp(character.getCurrentExp() + exp);
        if (character.getCurrentExp() >= character.getLevel() * 100) {
            character.setCurrentExp(character.getCurrentExp() - (character.getLevel() * 100));
            character.setLevel(character.getLevel() + 1);
            character.setMaxHp(character.getMaxHp() + 50);
            character.setCurrentHp(character.getMaxHp());
            character.setStatPoints(character.getStatPoints() + 5);
            res.setLevelUp(true);
        }

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
        c.setCurrentHp(1);
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

    @Transactional
    public BattleResult attackEnemy(Map<String, Object> payload) {
        return processTurn("ATTACK");
    }
}