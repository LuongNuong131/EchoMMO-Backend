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

    private final Random random = new Random();
    private static final double DROP_RATE = 0.3;

    // --- 1. START BATTLE ---
    @Transactional
    public BattleResult startBattle() {
        User user = getCurrentUser();
        Character character = charRepo.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Chưa tạo nhân vật"));

        // [FIX] Dùng cơ chế SaveOrUpdate thay vì Delete -> Insert để tránh lỗi khóa DB
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

        // Reset Session
        session.setEnemyId(enemy.getEnemyId());
        session.setEnemyName(enemy.getName());
        session.setEnemyMaxHp(enemy.getHp());
        session.setEnemyCurrentHp(enemy.getHp());
        session.setEnemyAtk(enemy.getAtk());
        session.setEnemyDef(enemy.getDef());
        session.setEnemySpeed(10);

        // Tính stats (Đã fix null pointer)
        int[] stats = calculatePlayerStats(character);
        session.setPlayerMaxHp(character.getMaxHp() + stats[3]); // HP Bonus
        session.setPlayerCurrentHp(session.getPlayerMaxHp());
        session.setPlayerCurrentEnergy(character.getEnergy());
        session.setCurrentTurn(0);
        session.setQteActive(false);

        sessionRepo.save(session);

        return buildResult(session, "Gặp " + enemy.getName() + "! Trận đấu bắt đầu.", "ONGOING");
    }

    // --- 2. PROCESS TURN ---
    @Transactional
    public BattleResult processTurn(String actionType) {
        User user = getCurrentUser();
        BattleSession session = sessionRepo.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Chưa vào trận! Hãy gọi /start"));

        Character character = charRepo.findByUser_UserId(user.getUserId()).get();
        List<String> logs = new ArrayList<>();

        if (session.getEnemyCurrentHp() <= 0) return handleWin(session, character);
        if (session.getPlayerCurrentHp() <= 0) return handleLoss(session);

        // A. QTE
        if (session.isQteActive()) {
            if ("BLOCK".equals(actionType)) {
                logs.add("🛡️ ĐỠ ĐÒN THÀNH CÔNG! (0 sát thương)");
                session.setQteActive(false);
                sessionRepo.save(session);
                return buildResult(session, logs, "ONGOING");
            } else {
                int def = calculatePlayerStats(character)[1];
                int dmg = Math.max(1, session.getEnemyAtk() - def);
                session.setPlayerCurrentHp(session.getPlayerCurrentHp() - dmg);
                logs.add("❌ Đỡ trượt! Bị đánh " + dmg + " máu.");
                session.setQteActive(false);

                if (session.getPlayerCurrentHp() <= 0) return handleLoss(session);
                sessionRepo.save(session);
                return buildResult(session, logs, "ONGOING");
            }
        }

        // B. TURN
        session.setCurrentTurn(session.getCurrentTurn() + 1);
        int[] stats = calculatePlayerStats(character);

        // Player Attack
        int pAtk = stats[0];
        int pCrit = stats[2];
        int dmgToEnemy = Math.max(1, pAtk - session.getEnemyDef());

        if (random.nextInt(100) < pCrit) {
            dmgToEnemy = (int)(dmgToEnemy * 1.5);
            logs.add("💥 BẠO KÍCH! Gây " + dmgToEnemy + " sát thương.");
        } else {
            logs.add("⚔️ Tấn công gây " + dmgToEnemy + " sát thương.");
        }
        session.setEnemyCurrentHp(session.getEnemyCurrentHp() - dmgToEnemy);

        if (session.getEnemyCurrentHp() <= 0) {
            sessionRepo.save(session);
            return handleWin(session, character);
        }

        // Enemy Attack
        if (random.nextInt(100) < 30) {
            session.setQteActive(true);
            session.setQteExpiryTime(LocalDateTime.now().plusSeconds(2));
            logs.add("⚠️ " + session.getEnemyName() + " tung chiêu! ĐỠ NGAY!");
            sessionRepo.save(session);
            BattleResult res = buildResult(session, logs, "QTE_ACTION");
            res.setQteTriggered(true);
            return res;
        }

        int pDef = stats[1];
        int dmgToPlayer = Math.max(1, session.getEnemyAtk() - pDef);
        session.setPlayerCurrentHp(session.getPlayerCurrentHp() - dmgToPlayer);
        logs.add("👾 " + session.getEnemyName() + " đánh trả " + dmgToPlayer + " máu.");

        if (session.getPlayerCurrentHp() <= 0) return handleLoss(session);

        sessionRepo.save(session);
        return buildResult(session, logs, "ONGOING");
    }

    // --- HELPER FIX NULL POINTER ---
    private int[] calculatePlayerStats(Character c) {
        List<UserItem> items = userItemRepo.findByUser_UserIdAndIsEquippedTrue(c.getUser().getUserId());
        int atk = c.getBaseAtk();
        int def = c.getBaseDef();
        int crit = c.getBaseCritRate();
        int hpBonus = 0;

        for (UserItem ui : items) {
            Item item = ui.getItem();
            if (item != null) {
                // [FIX] Kiểm tra null trước khi cộng
                atk += (item.getAtkBonus() != null) ? item.getAtkBonus() : 0;
                def += (item.getDefBonus() != null) ? item.getDefBonus() : 0;
                crit += (item.getCritRateBonus() != null) ? item.getCritRateBonus() : 0;
                hpBonus += (item.getHpBonus() != null) ? item.getHpBonus() : 0;
            }
        }
        return new int[]{atk, def, crit, hpBonus};
    }

    // --- OTHER HELPERS ---
    @Transactional
    public BattleResult attackEnemy(Map<String, Object> payload) {
        return processTurn("ATTACK");
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

        character.setExp(character.getExp() + exp);
        if (character.getExp() >= character.getLv() * 100L) {
            character.setExp(character.getExp() - (int)(character.getLv() * 100L));
            character.setLv(character.getLv() + 1);
            character.setMaxHp(character.getMaxHp() + 20);
            res.setLevelUp(true);
        }

        Wallet w = walletRepo.findByUser_UserId(character.getUser().getUserId()).orElse(null);
        if (w != null) {
            w.setGold(w.getGold().add(BigDecimal.valueOf(gold)));
            walletRepo.save(w);
        }

        character.setHp(Math.max(1, session.getPlayerCurrentHp()));
        charRepo.save(character);

        res.setExpEarned(exp);
        res.setGoldEarned(gold);

        handleNewItemDrop(character.getUser(), res.getCombatLog(), res, (enemyRef != null ? enemyRef.getLevel() : 1));

        sessionRepo.delete(session);
        return res;
    }

    private BattleResult handleLoss(BattleSession session) {
        BattleResult res = buildResult(session, "💀 Thất bại...", "DEFEAT");
        Character c = charRepo.findByUser_UserId(session.getUser().getUserId()).get();
        c.setHp(1); charRepo.save(c); sessionRepo.delete(session);
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

    private void handleNewItemDrop(User user, List<String> logs, BattleResult result, int enemyLv) {
        if (random.nextDouble() > DROP_RATE) return;
        String type = Arrays.asList("WEAPON", "ARMOR", "HELMET", "BOOTS", "RING", "NECKLACE").get(random.nextInt(6));
        String rarity = (random.nextInt(100) < 20) ? "A" : "C";
        Item item = new Item();
        item.setUser(user); item.setType(type); item.setRarity(rarity); item.setName(rarity + " " + type);
        item.setBasePrice(BigDecimal.valueOf(100));
        item.setImageUrl("s_sword_0.png");
        // Init stats để không bị null lần sau
        item.setAtkBonus(0); item.setDefBonus(0); item.setHpBonus(0); item.setCritRateBonus(0); item.setSpeedBonus(0); item.setEnergyBonus(0);
        itemRepo.save(item);
        UserItem ui = new UserItem(); ui.setUser(user); ui.setItem(item); ui.setQuantity(1);
        userItemRepo.save(ui);
        logs.add("🎁 Nhặt được: " + item.getName());
        result.setDroppedItemName(item.getName()); result.setDroppedItemImage(item.getImageUrl()); result.setDroppedItemRarity(rarity);
    }

    public List<Skill> getAllSkills() { return new ArrayList<>(); }
}