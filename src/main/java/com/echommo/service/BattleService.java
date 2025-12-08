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

    // [QUAN TRỌNG] Phải có Repository này để lưu trạng thái trận đấu
    @Autowired private BattleSessionRepository sessionRepo;

    private final Random random = new Random();
    private static final double DROP_RATE = 0.3;

    // --- 1. START BATTLE ---
    @Transactional
    public BattleResult startBattle() {
        User user = getCurrentUser();
        Character character = charRepo.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Chưa tạo nhân vật"));

        // Xóa trận cũ nếu có
        sessionRepo.findByUser_UserId(user.getUserId()).ifPresent(sessionRepo::delete);

        // Tìm quái
        List<Enemy> enemies = enemyRepo.findAll();
        Enemy enemy = enemies.isEmpty() ? createDummyEnemy() : enemies.get(random.nextInt(enemies.size()));

        // Tạo Session mới
        BattleSession session = new BattleSession();
        session.setUser(user);
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
        session.setPlayerCurrentHp(session.getPlayerMaxHp());
        session.setPlayerCurrentEnergy(character.getEnergy());

        sessionRepo.save(session);

        return buildResult(session, "Gặp " + enemy.getName() + "! Trận đấu bắt đầu.", "ONGOING");
    }

    // --- 2. PROCESS TURN (Logic đánh theo lượt) ---
    @Transactional
    public BattleResult processTurn(String actionType) {
        User user = getCurrentUser();
        BattleSession session = sessionRepo.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Không có trận đấu nào! Hãy gọi /start trước."));

        Character character = charRepo.findByUser_UserId(user.getUserId()).get();
        List<String> logs = new ArrayList<>();

        // Check thắng/thua ngay đầu lượt
        if (session.getEnemyCurrentHp() <= 0) return handleWin(session, character);
        if (session.getPlayerCurrentHp() <= 0) return handleLoss(session);

        // A. XỬ LÝ QTE (Đỡ đòn)
        if (session.isQteActive()) {
            if ("BLOCK".equals(actionType)) {
                logs.add("🛡️ ĐỠ ĐÒN THÀNH CÔNG! Chặn hoàn toàn sát thương.");
                session.setQteActive(false);
                sessionRepo.save(session);
                return buildResult(session, logs, "ONGOING");
            } else {
                int dmg = Math.max(1, session.getEnemyAtk() - calculatePlayerStats(character)[1]);
                session.setPlayerCurrentHp(session.getPlayerCurrentHp() - dmg);
                logs.add("❌ Bấm trượt! Nhận " + dmg + " sát thương.");
                session.setQteActive(false);

                if (session.getPlayerCurrentHp() <= 0) return handleLoss(session);
                sessionRepo.save(session);
                return buildResult(session, logs, "ONGOING");
            }
        }

        // B. LOGIC ĐÁNH THƯỜNG
        session.setCurrentTurn(session.getCurrentTurn() + 1);
        int[] stats = calculatePlayerStats(character);
        int pAtk = stats[0];
        int pCrit = stats[2];

        // 1. Player đánh
        int dmgToEnemy = Math.max(1, pAtk - session.getEnemyDef());
        if (random.nextInt(100) < pCrit) {
            dmgToEnemy = (int)(dmgToEnemy * 1.5);
            logs.add("💥 BẠO KÍCH! Bạn gây " + dmgToEnemy + " sát thương.");
        } else {
            logs.add("⚔️ Bạn tấn công gây " + dmgToEnemy + " sát thương.");
        }
        session.setEnemyCurrentHp(session.getEnemyCurrentHp() - dmgToEnemy);

        if (session.getEnemyCurrentHp() <= 0) {
            sessionRepo.save(session);
            return handleWin(session, character);
        }

        // 2. Enemy đánh (Có tỉ lệ kích hoạt QTE)
        if (random.nextInt(100) < 30) { // 30% ra QTE
            session.setQteActive(true);
            session.setQteExpiryTime(LocalDateTime.now().plusSeconds(2));
            logs.add("⚠️ " + session.getEnemyName() + " tung đòn mạnh! ĐỠ NGAY!");
            sessionRepo.save(session);

            BattleResult res = buildResult(session, logs, "QTE_ACTION");
            res.setQteTriggered(true);
            return res;
        }

        // Nếu không QTE -> Đánh thường
        int dmgToPlayer = Math.max(1, session.getEnemyAtk() - stats[1]);
        session.setPlayerCurrentHp(session.getPlayerCurrentHp() - dmgToPlayer);
        logs.add("👾 " + session.getEnemyName() + " đánh trả " + dmgToPlayer + " sát thương.");

        if (session.getPlayerCurrentHp() <= 0) return handleLoss(session);

        sessionRepo.save(session);
        return buildResult(session, logs, "ONGOING");
    }

    // --- 3. HÀM TƯƠNG THÍCH (SỬA LỖI CỦA BẠN) ---
    // Controller đang gọi hàm này, nên ta giữ lại và trỏ nó vào processTurn
    @Transactional
    public BattleResult attackEnemy(Map<String, Object> payload) {
        // Mặc định là hành động tấn công thường
        return processTurn("ATTACK");
    }

    public List<Skill> getAllSkills() { return new ArrayList<>(); }

    // --- CÁC HÀM PHỤ TRỢ ---
    private BattleResult handleWin(BattleSession session, Character character) {
        BattleResult res = buildResult(session, "🏆 Chiến thắng!", "VICTORY");
        Enemy enemy = enemyRepo.findById(session.getEnemyId()).orElse(null);
        int exp = (enemy != null) ? enemy.getExpReward() : 10;
        int gold = (enemy != null) ? enemy.getGoldReward() : 10;

        character.setExp(character.getExp() + exp);

        // Level Up Check
        if (character.getExp() >= character.getLv() * 100L) {
            character.setExp(character.getExp() - (int)(character.getLv() * 100L));
            character.setLv(character.getLv() + 1);
            character.setMaxHp(character.getMaxHp() + 20);
            res.setLevelUp(true);
        }

        // Cộng tiền
        Wallet w = walletRepo.findByUser_UserId(character.getUser().getUserId()).orElse(null);
        if (w != null) {
            w.setGold(w.getGold().add(BigDecimal.valueOf(gold)));
            walletRepo.save(w);
        }

        // Cập nhật HP thật
        character.setHp(Math.max(1, session.getPlayerCurrentHp()));
        charRepo.save(character);

        res.setExpEarned(exp);
        res.setGoldEarned(gold);

        // Drop đồ
        handleNewItemDrop(character.getUser(), res.getCombatLog(), res, (enemy != null ? enemy.getLevel() : 1));

        sessionRepo.delete(session);
        return res;
    }

    private BattleResult handleLoss(BattleSession session) {
        BattleResult res = buildResult(session, "💀 Bạn đã thất bại...", "DEFEAT");
        Character c = charRepo.findByUser_UserId(session.getUser().getUserId()).get();
        c.setHp(1);
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
        List<String> logs = new ArrayList<>();
        logs.add(msg);
        return buildResult(s, logs, status);
    }

    private int[] calculatePlayerStats(Character c) {
        List<UserItem> items = userItemRepo.findByUser_UserIdAndIsEquippedTrue(c.getUser().getUserId());
        int atk = c.getBaseAtk();
        int def = c.getBaseDef();
        int crit = c.getBaseCritRate();
        int hpBonus = 0;
        for (UserItem ui : items) {
            atk += ui.getItem().getAtkBonus();
            def += ui.getItem().getDefBonus();
            crit += ui.getItem().getCritRateBonus();
            hpBonus += ui.getItem().getHpBonus();
        }
        return new int[]{atk, def, crit, hpBonus};
    }

    private User getCurrentUser() {
        return userRepo.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Enemy createDummyEnemy() {
        Enemy e = new Enemy(); e.setEnemyId(1); e.setName("Bù Nhìn"); e.setHp(100);
        e.setAtk(5); e.setDef(0); return e;
    }

    // --- DROP ITEM LOGIC (GIỮ NGUYÊN TỪ CŨ) ---
    private void handleNewItemDrop(User user, List<String> logs, BattleResult result, int enemyLv) {
        if (random.nextDouble() > DROP_RATE) return;
        String type = pickRandomType();
        String rarity = pickRarity();
        Item item = new Item();
        item.setUser(user); item.setType(type); item.setRarity(rarity);
        item.setBasePrice(BigDecimal.valueOf(100 + (enemyLv * 10L)));
        applyStats(item, type, rarity, enemyLv);
        item.setName(generateName(type, rarity));
        item.setImageUrl(generateImageCode(type));
        item.setDescription("Rơi từ quái cấp " + enemyLv);
        itemRepo.save(item);
        UserItem ui = new UserItem(); ui.setUser(user); ui.setItem(item); ui.setQuantity(1); ui.setIsEquipped(false);
        userItemRepo.save(ui);
        logs.add("🎁 RƠI ĐỒ: [" + item.getName() + "]");
        result.setDroppedItemName(item.getName());
        result.setDroppedItemImage(item.getImageUrl());
        result.setDroppedItemRarity(rarity);
    }

    private String pickRandomType() { return Arrays.asList("WEAPON", "ARMOR", "HELMET", "BOOTS", "RING", "NECKLACE").get(random.nextInt(6)); }
    private String pickRarity() { int r = random.nextInt(100); return r<5?"S":r<20?"A":r<50?"B":"C"; }
    private void applyStats(Item item, String type, String rarity, int lv) { /* Logic stats cũ của bạn... */ }
    private String generateName(String type, String rarity) { return rarity + " " + type; }
    private String generateImageCode(String type) { return "s_sword_0.png"; } // Demo
}