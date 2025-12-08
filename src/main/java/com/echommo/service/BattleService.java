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

    // --- 1. START BATTLE (LOGIC UPDATE AN TOÀN) ---
    @Transactional
    public BattleResult startBattle() {
        User user = getCurrentUser();
        Character character = charRepo.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Chưa tạo nhân vật"));

        // [FIX QUAN TRỌNG] Tìm session cũ để Update, không Delete để tránh lỗi khóa ngoại
        BattleSession session = sessionRepo.findByUser_UserId(user.getUserId())
                .orElse(new BattleSession());

        if (session.getId() == null) {
            session.setUser(user); // Gán user nếu là session mới
        }

        // Lấy danh sách quái từ DB (Bạn đã seed 3 con: Yêu Tinh, Nấm Độc, Bộ Xương)
        List<Enemy> enemies = enemyRepo.findAll();
        Enemy enemy;
        if (enemies.isEmpty()) {
            // Fallback nếu lỡ tay xóa DB (tạo con dummy)
            enemy = new Enemy(); enemy.setEnemyId(0); enemy.setName("Bù Nhìn");
            enemy.setHp(100); enemy.setAtk(5); enemy.setDef(0);
        } else {
            // Random quái thật
            enemy = enemies.get(random.nextInt(enemies.size()));
        }

        // Reset thông số Session
        session.setEnemyId(enemy.getEnemyId());
        session.setEnemyName(enemy.getName());
        session.setEnemyMaxHp(enemy.getHp());
        session.setEnemyCurrentHp(enemy.getHp());
        session.setEnemyAtk(enemy.getAtk());
        session.setEnemyDef(enemy.getDef());
        session.setEnemySpeed(10);

        // Tính chỉ số Player
        int[] stats = calculatePlayerStats(character);
        session.setPlayerMaxHp(character.getMaxHp() + stats[3]);
        session.setPlayerCurrentHp(session.getPlayerMaxHp()); // Hồi đầy máu
        session.setPlayerCurrentEnergy(character.getEnergy());
        session.setCurrentTurn(0);
        session.setQteActive(false); // Reset QTE

        sessionRepo.save(session); // Lưu xuống DB

        return buildResult(session, "Gặp " + enemy.getName() + "! Trận đấu bắt đầu.", "ONGOING");
    }

    // --- 2. PROCESS TURN ---
    @Transactional
    public BattleResult processTurn(String actionType) {
        User user = getCurrentUser();
        BattleSession session = sessionRepo.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Chưa vào trận!"));

        Character character = charRepo.findByUser_UserId(user.getUserId()).get();
        List<String> logs = new ArrayList<>();

        // 1. Check kết quả trận đấu trước khi đánh
        if (session.getEnemyCurrentHp() <= 0) return handleWin(session, character);
        if (session.getPlayerCurrentHp() <= 0) return handleLoss(session);

        // 2. Xử lý QTE (Đỡ đòn)
        if (session.isQteActive()) {
            if ("BLOCK".equals(actionType)) {
                logs.add("🛡️ ĐỠ ĐÒN THÀNH CÔNG! (0 sát thương)");
                session.setQteActive(false);
                sessionRepo.save(session);
                return buildResult(session, logs, "ONGOING");
            } else {
                // Bị đánh trúng
                int dmg = Math.max(1, session.getEnemyAtk() - calculatePlayerStats(character)[1]);
                session.setPlayerCurrentHp(session.getPlayerCurrentHp() - dmg);
                logs.add("❌ Đỡ trượt! Bị " + session.getEnemyName() + " vả " + dmg + " máu.");
                session.setQteActive(false);

                if (session.getPlayerCurrentHp() <= 0) return handleLoss(session);
                sessionRepo.save(session);
                return buildResult(session, logs, "ONGOING");
            }
        }

        // 3. Logic Turn (Đánh thường)
        session.setCurrentTurn(session.getCurrentTurn() + 1);
        int[] stats = calculatePlayerStats(character);

        // PLAYER ĐÁNH
        int dmgToEnemy = Math.max(1, stats[0] - session.getEnemyDef()); // Atk - Def
        boolean isCrit = random.nextInt(100) < stats[2]; // Crit Rate
        if (isCrit) {
            dmgToEnemy = (int)(dmgToEnemy * 1.5);
            logs.add("💥 BẠO KÍCH! Bạn gây " + dmgToEnemy + " sát thương.");
        } else {
            logs.add("⚔️ Tấn công gây " + dmgToEnemy + " sát thương.");
        }
        session.setEnemyCurrentHp(session.getEnemyCurrentHp() - dmgToEnemy);

        if (session.getEnemyCurrentHp() <= 0) {
            sessionRepo.save(session);
            return handleWin(session, character);
        }

        // ENEMY ĐÁNH TRẢ (30% ra QTE)
        if (random.nextInt(100) < 30) {
            session.setQteActive(true);
            session.setQteExpiryTime(LocalDateTime.now().plusSeconds(2));
            logs.add("⚠️ " + session.getEnemyName() + " tung tuyệt chiêu! ĐỠ NGAY!");
            sessionRepo.save(session);

            BattleResult res = buildResult(session, logs, "QTE_ACTION");
            res.setQteTriggered(true);
            return res;
        }

        // Enemy đánh thường
        int dmgToPlayer = Math.max(1, session.getEnemyAtk() - stats[1]);
        session.setPlayerCurrentHp(session.getPlayerCurrentHp() - dmgToPlayer);
        logs.add("👾 " + session.getEnemyName() + " cào nhẹ " + dmgToPlayer + " máu.");

        if (session.getPlayerCurrentHp() <= 0) return handleLoss(session);

        sessionRepo.save(session);
        return buildResult(session, logs, "ONGOING");
    }

    // --- BRIDGE METHOD CHO CONTROLLER ---
    @Transactional
    public BattleResult attackEnemy(Map<String, Object> payload) {
        return processTurn("ATTACK");
    }

    public List<Skill> getAllSkills() { return new ArrayList<>(); }

    // --- PRIVATE HELPERS ---
    private BattleResult handleWin(BattleSession session, Character character) {
        BattleResult res = buildResult(session, "🏆 Đã tiêu diệt " + session.getEnemyName() + "!", "VICTORY");

        // Lấy thông tin quái gốc để tính thưởng
        Enemy enemyRef = enemyRepo.findById(session.getEnemyId()).orElse(null);
        int exp = (enemyRef != null) ? enemyRef.getExpReward() : 10;
        int gold = (enemyRef != null) ? enemyRef.getGoldReward() : 10;

        character.setExp(character.getExp() + exp);
        // Level Up Logic
        if (character.getExp() >= character.getLv() * 100L) {
            character.setExp(character.getExp() - (int)(character.getLv() * 100L));
            character.setLv(character.getLv() + 1);
            character.setMaxHp(character.getMaxHp() + 20);
            res.setLevelUp(true);
        }

        // Cộng vàng
        Wallet w = walletRepo.findByUser_UserId(character.getUser().getUserId()).orElse(null);
        if (w != null) {
            w.setGold(w.getGold().add(BigDecimal.valueOf(gold)));
            walletRepo.save(w);
        }

        // Cập nhật HP thật cho nhân vật sau trận
        character.setHp(Math.max(1, session.getPlayerCurrentHp()));
        charRepo.save(character);

        res.setExpEarned(exp);
        res.setGoldEarned(gold);

        handleNewItemDrop(character.getUser(), res.getCombatLog(), res, (enemyRef != null ? enemyRef.getLevel() : 1));

        // Xóa session sau khi thắng
        sessionRepo.delete(session);
        return res;
    }

    private BattleResult handleLoss(BattleSession session) {
        BattleResult res = buildResult(session, "💀 Bạn đã bị đánh bại...", "DEFEAT");
        Character c = charRepo.findByUser_UserId(session.getUser().getUserId()).get();
        c.setHp(1); // Về làng với 1 máu
        charRepo.save(c);
        sessionRepo.delete(session);
        return res;
    }

    private BattleResult buildResult(BattleSession s, List<String> logs, String status) {
        BattleResult res = new BattleResult();
        res.setEnemyId(s.getEnemyId());
        res.setEnemyName(s.getEnemyName());
        res.setEnemyHp(s.getEnemyCurrentHp());
        res.setEnemyMaxHp(s.getEnemyMaxHp());
        res.setPlayerHp(s.getPlayerCurrentHp());
        res.setPlayerMaxHp(s.getPlayerMaxHp());
        res.setPlayerEnergy(s.getPlayerCurrentEnergy());
        res.setCombatLog(logs);
        res.setStatus(status);
        return res;
    }

    private BattleResult buildResult(BattleSession s, String msg, String status) {
        List<String> logs = new ArrayList<>(); logs.add(msg);
        return buildResult(s, logs, status);
    }

    private int[] calculatePlayerStats(Character c) {
        List<UserItem> items = userItemRepo.findByUser_UserIdAndIsEquippedTrue(c.getUser().getUserId());
        int atk = c.getBaseAtk(); int def = c.getBaseDef(); int crit = c.getBaseCritRate(); int hpBonus = 0;
        for (UserItem ui : items) {
            atk += ui.getItem().getAtkBonus(); def += ui.getItem().getDefBonus();
            crit += ui.getItem().getCritRateBonus(); hpBonus += ui.getItem().getHpBonus();
        }
        return new int[]{atk, def, crit, hpBonus};
    }

    private User getCurrentUser() {
        return userRepo.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Drop Item Logic (Giữ nguyên)
    private void handleNewItemDrop(User user, List<String> logs, BattleResult result, int enemyLv) {
        if (random.nextDouble() > DROP_RATE) return;
        String type = Arrays.asList("WEAPON", "ARMOR", "HELMET", "BOOTS", "RING", "NECKLACE").get(random.nextInt(6));
        String rarity = (random.nextInt(100) < 20) ? "A" : "C"; // Demo tỉ lệ
        Item item = new Item();
        item.setUser(user); item.setType(type); item.setRarity(rarity); item.setName(rarity + " " + type);
        item.setBasePrice(BigDecimal.valueOf(100));
        item.setImageUrl("s_sword_0.png"); // Placeholder
        itemRepo.save(item);
        UserItem ui = new UserItem(); ui.setUser(user); ui.setItem(item); ui.setQuantity(1);
        userItemRepo.save(ui);
        logs.add("🎁 Nhặt được: " + item.getName());
        result.setDroppedItemName(item.getName()); result.setDroppedItemImage(item.getImageUrl()); result.setDroppedItemRarity(rarity);
    }
}