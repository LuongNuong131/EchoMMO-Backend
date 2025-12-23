package com.echommo.service;

import com.echommo.dto.BattleAttackRequest;
import com.echommo.dto.BattleResult;
import com.echommo.entity.*;
import com.echommo.entity.Character;
import com.echommo.enums.CharacterStatus;
import com.echommo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BattleService {

    private final CharacterRepository charRepo;
    private final EnemyRepository enemyRepo;
    private final WalletRepository walletRepo;
    private final UserRepository userRepo;
    private final BattleSessionRepository sessionRepo;
    private final CharacterService charService; // Cần service này để tính stats

    // Drop Item Repos
    private final ItemRepository itemRepo;
    private final UserItemRepository userItemRepo;
    private final ItemGenerationService itemGenService;

    private final Random random = new Random();

    // --- HELPERS ---
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Lỗi xác thực người dùng."));
    }

    private Character getMyCharacter() {
        User user = getCurrentUser();
        return charRepo.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Chưa tạo nhân vật!"));
    }

    // --- CHIẾN ĐẤU ---

    @Transactional
    public BattleResult startBattle() {
        Character character = getMyCharacter();

        // [FIX] Tính lại stats ngay khi bắt đầu trận để đảm bảo máu/giáp đúng với đồ đang mặc
        charService.recalculateStats(character);

        List<BattleSession> sessions = sessionRepo.findByCharacter_CharId(character.getCharId());

        if (sessions.isEmpty()) {
            if (character.getStatus() == CharacterStatus.IN_COMBAT) {
                character.setStatus(CharacterStatus.IDLE);
                charRepo.save(character);
            }
            throw new RuntimeException("Không tìm thấy trận đấu nào! Hãy đi thám hiểm trước.");
        }

        BattleSession session = sessions.get(0);
        character.setStatus(CharacterStatus.IN_COMBAT);
        charRepo.save(character);

        String message = "⚔️ Bạn chạm trán " + session.getEnemyName() + "!";
        return buildResult(session, Collections.singletonList(message), "ONGOING");
    }

    /**
     * Xử lý lượt đánh (Turn).
     * [CORE FIX] Server tự tính stats, không cần Client gửi lên.
     */
    @Transactional
    public BattleResult processTurn(BattleAttackRequest req) {
        Character character = getMyCharacter();

        // [QUAN TRỌNG] Gọi hàm này để Server cộng dồn chỉ số từ đồ vào Character Entity
        // Lúc này character.getBaseDef() sẽ là 1000 chứ không phải 7 nữa.
        charService.recalculateStats(character);

        List<BattleSession> sessions = sessionRepo.findByCharacter_CharId(character.getCharId());
        if (sessions.isEmpty()) throw new RuntimeException("Trận đấu đã kết thúc hoặc không tồn tại.");

        BattleSession s = sessions.get(0);
        List<String> logs = new ArrayList<>();
        s.setCurrentTurn(s.getCurrentTurn() + 1);

        // --- 1. LẤY CHỈ SỐ THỰC TẾ (Đã được recalculateStats tính toán) ---
        int pAtk = character.getBaseAtk();
        int pDef = character.getBaseDef();
        int pSpeed = character.getBaseSpeed();
        int pCrit = character.getBaseCritRate();
        int pCritDmg = character.getBaseCritDmg();

        // --- 2. NGƯỜI CHƠI TẤN CÔNG ---
        int eDef = s.getEnemyDef();

        // Tính né tránh quái (Dựa trên chênh lệch Speed thực tế)
        int eDodge = Math.min(60, Math.max(0, 5 + ((s.getEnemySpeed() != null ? s.getEnemySpeed() : 10) - pSpeed)));

        if (random.nextInt(100) < eDodge) {
            logs.add("💨 " + s.getEnemyName() + " né được đòn!");
        } else {
            // Dame người chơi: (Atk - EnemyDef)
            int dmg = Math.max((int)Math.ceil(pAtk * 0.1), pAtk - eDef);

            // Xử lý Buff
            if (Boolean.TRUE.equals(req.getIsBuffed())) {
                dmg = (int)(dmg * 1.5);
                logs.add("💪 TỤ LỰC! Sát thương tăng cường.");
            }

            // Xử lý Bạo kích
            if (random.nextInt(100) < pCrit) {
                dmg = (int)(dmg * (pCritDmg / 100.0));
                logs.add("🔥 BẠO KÍCH! Gây " + dmg + " sát thương.");
            } else {
                logs.add("⚔️ Gây " + dmg + " sát thương.");
            }
            s.setEnemyCurrentHp(Math.max(0, s.getEnemyCurrentHp() - dmg));
        }

        if (s.getEnemyCurrentHp() <= 0) return handleWin(s, character, logs);

        // --- 3. QUÁI TẤN CÔNG ---
        int eAtk = s.getEnemyAtk();

        // Tính né tránh người chơi
        int pDodge = Math.min(50, Math.max(0, (pSpeed - (s.getEnemySpeed() != null ? s.getEnemySpeed() : 10)) / 2));

        if (random.nextInt(100) < pDodge) {
            logs.add("✨ Bạn né đòn thành công!");
        } else {
            // [FIX LỖI 11 MÁU]: Bây giờ pDef đã là 1000 (nhờ recalculateStats)
            int dmgTaken = eAtk - pDef;

            // Nếu Giáp > Công quái => Damage = 1 hoặc 0 (Miss)
            if (dmgTaken <= 0) {
                dmgTaken = 1; // Hoặc set = 0 nếu muốn đánh Miss hoàn toàn
                logs.add("🛡️ Giáp quá cứng! Chỉ mất " + dmgTaken + " HP.");
            } else {
                logs.add("💔 Bị đánh trúng, mất " + dmgTaken + " HP.");
            }

            s.setPlayerCurrentHp(Math.max(0, s.getPlayerCurrentHp() - dmgTaken));
            character.setCurrentHp(s.getPlayerCurrentHp());
        }

        if (s.getPlayerCurrentHp() <= 0) return handleLoss(s, character, logs);

        sessionRepo.save(s);
        charRepo.save(character);
        return buildResult(s, logs, "ONGOING");
    }

    // --- CÁC HÀM XỬ LÝ THẮNG/THUA & ITEM DROP (GIỮ NGUYÊN) ---

    private BattleResult handleWin(BattleSession session, Character character, List<String> logs) {
        Enemy enemy = enemyRepo.findById(session.getEnemyId()).orElse(new Enemy());
        int enemyLvl = enemy.getLevel() != null ? enemy.getLevel() : 1;

        int expReward = (enemy.getExpReward() != null ? enemy.getExpReward() : 10 * enemyLvl);
        int goldReward = (enemy.getGoldReward() != null ? enemy.getGoldReward() : 5 * enemyLvl);

        boolean isElite = session.getEnemyName().contains("[Tinh Anh]");
        if (isElite) {
            expReward *= 3;
            goldReward *= 3;
        }

        character.setCurrentExp(character.getCurrentExp() + expReward);
        character.setMonsterKills(character.getMonsterKills() + 1);
        character.setStatus(CharacterStatus.IDLE);
        checkLevelUp(character);

        Wallet wallet = character.getUser().getWallet();
        wallet.setGold(wallet.getGold().add(BigDecimal.valueOf(goldReward)));

        if (isElite && random.nextInt(100) < 25) {
            wallet.setEchoCoin(wallet.getEchoCoin().add(new BigDecimal("0.1")));
            logs.add("💎 [HIẾM] Nhận 0.1 Echo Coin!");
        } else if (random.nextInt(100) < 5) {
            wallet.setEchoCoin(wallet.getEchoCoin().add(new BigDecimal("0.01")));
            logs.add("💎 Nhận 0.01 Echo Coin!");
        }

        BattleResult result = buildResult(session, logs, "VICTORY");
        result.setGoldEarned(goldReward);
        result.setExpEarned(expReward);

        int dropChance = isElite ? 40 : 20;
        if (random.nextInt(100) < dropChance) {
            processItemDrop(character, enemy, isElite, result, logs);
        }

        int heal = (int)(character.getMaxHp() * 0.05);
        character.setCurrentHp(Math.min(character.getMaxHp(), character.getCurrentHp() + heal));

        walletRepo.save(wallet);
        charRepo.save(character);
        sessionRepo.delete(session);

        logs.add("🏆 CHIẾN THẮNG! +" + expReward + " EXP, +" + goldReward + " Vàng.");
        return result;
    }

    private void processItemDrop(Character c, Enemy enemy, boolean isElite, BattleResult result, List<String> logs) {
        int enemyLvl = enemy.getLevel() != null ? enemy.getLevel() : 1;
        int maxAllowedTier = Math.max(1, (int) Math.ceil(enemyLvl / 10.0));

        List<Item> candidates = itemRepo.findAll().stream()
                .filter(item -> {
                    int t = item.getTier() != null ? item.getTier() : 1;
                    return t <= maxAllowedTier;
                })
                .collect(Collectors.toList());

        if (candidates.isEmpty()) return;

        Item droppedItem = candidates.get(random.nextInt(candidates.size()));

        if (isInventoryFull(c, droppedItem)) {
            if (isEquipment(droppedItem)) {
                fillDropResult(result, droppedItem, true);
                logs.add("⚠️ Túi đầy! Phát hiện: " + droppedItem.getName());
            } else {
                logs.add("❌ Túi đầy! Không thể nhặt " + droppedItem.getName());
            }
            return;
        }

        grantItemToUser(c, droppedItem);
        fillDropResult(result, droppedItem, false);
        logs.add("🎁 Nhặt được: " + droppedItem.getName());
    }

    private boolean isInventoryFull(Character c, Item item) {
        int maxSlots = c.getUser().getInventorySlots() != null ? c.getUser().getInventorySlots() : 50;
        if (!isEquipment(item)) {
            Optional<UserItem> exist = userItemRepo.findByCharacter_CharIdAndItem_ItemId(c.getCharId(), item.getItemId())
                    .stream().filter(ui -> !ui.getIsEquipped()).findFirst();
            if (exist.isPresent()) return false;
        }
        int currentSlots = userItemRepo.countByCharacter_CharId(c.getCharId());
        return currentSlots >= maxSlots;
    }

    private boolean isEquipment(Item item) {
        return List.of("WEAPON", "ARMOR", "TOOL", "NECKLACE", "RING", "HELMET", "BOOTS").contains(item.getType());
    }

    private void grantItemToUser(Character c, Item item) {
        if (!isEquipment(item)) {
            Optional<UserItem> exist = userItemRepo.findByCharacter_CharIdAndItem_ItemId(c.getCharId(), item.getItemId())
                    .stream().filter(ui -> !ui.getIsEquipped()).findFirst();
            if (exist.isPresent()) {
                exist.get().setQuantity(exist.get().getQuantity() + 1);
                userItemRepo.save(exist.get());
                return;
            }
        }

        UserItem ui = new UserItem();
        ui.setCharacter(c);
        ui.setItem(item);
        ui.setQuantity(1);
        ui.setIsEquipped(false);
        ui.setEnhanceLevel(0);
        ui.setMythicStars(0);
        ui.setAcquiredAt(LocalDateTime.now());
        ui.setMaxDurability(item.getMaxDurability() != null ? item.getMaxDurability() : 100);
        ui.setCurrentDurability(ui.getMaxDurability());
        ui.setMainStatValue(BigDecimal.valueOf(item.getBaseMainStat() != null ? item.getBaseMainStat() : 0));

        if (isEquipment(item)) {
            itemGenService.randomizeNewItem(ui);
        }
        userItemRepo.save(ui);
    }

    private void fillDropResult(BattleResult res, Item item, boolean invFull) {
        res.setHasDrop(true);
        res.setDropName(item.getName());
        res.setDropImage(item.getImageUrl() != null ? item.getImageUrl() : "item_box.png");
        res.setDropRarity(item.getRarity() != null ? item.getRarity().name() : "COMMON");
        res.setInventoryFull(invFull);
        res.setDroppedItemName(item.getName());
        res.setDroppedItemImage(item.getImageUrl());
        res.setDroppedItemRarity(item.getRarity() != null ? item.getRarity().name() : "COMMON");
    }

    private BattleResult handleLoss(BattleSession session, Character character, List<String> logs) {
        character.setStatus(CharacterStatus.IDLE);
        character.setCurrentHp(10);
        charRepo.save(character);
        sessionRepo.delete(session);
        logs.add("💀 BẠI TRẬN trước " + session.getEnemyName());
        return buildResult(session, logs, "DEFEAT");
    }

    private void checkLevelUp(Character c) {
        long required = c.getLevel() * 150L;
        if (c.getCurrentExp() >= required) {
            c.setLevel(c.getLevel() + 1);
            c.setCurrentExp(c.getCurrentExp() - required);
            charService.recalculateStats(c); // Quan trọng: Tính lại stats khi lên cấp
            c.setCurrentHp(c.getMaxHp());
            c.setCurrentEnergy(c.getMaxEnergy());
        }
    }

    private BattleResult buildResult(BattleSession s, List<String> logs, String status) {
        BattleResult res = new BattleResult();
        res.setEnemyName(s.getEnemyName());
        res.setEnemyHp(s.getEnemyCurrentHp());
        res.setEnemyMaxHp(s.getEnemyMaxHp());
        res.setPlayerHp(s.getPlayerCurrentHp());
        res.setPlayerMaxHp(s.getPlayerMaxHp());
        res.setCombatLog(logs);
        res.setStatus(status);
        return res;
    }
}