package com.echommo.service;

import com.echommo.dto.BattleResult;
import com.echommo.entity.*;
import com.echommo.entity.Character;
import com.echommo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Service
public class BattleService {

    @Autowired private CharacterService characterService;
    @Autowired private EnemyRepository enemyRepository; // Cần EnemyRepository
    @Autowired private CharacterRepository characterRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private SkillRepository skillRepository; // Cần SkillRepository
    @Autowired private QuestService questService; // Cần QuestService
    @Autowired private ItemRepository itemRepository;
    @Autowired private UserItemRepository userItemRepository; // Cần UserItemRepository
    @Autowired private CaptchaService captchaService; // Cần CaptchaService
    @Autowired private UserRepository userRepository;

    private final Random random = new Random();

    public BattleResult startBattle() {
        Character player = characterService.getMyCharacter();
        User user = player.getUser();

        // 1. Check Captcha (Chống Auto)
        captchaService.checkLockStatus(user);
        if (Boolean.TRUE.equals(user.getIsCaptchaLocked())) {
            throw new RuntimeException("CAPTCHA_REQUIRED");
        }
        // Tỷ lệ 1% hiện Captcha
        if (random.nextInt(100) < 1) {
            user.setIsCaptchaLocked(true);
            userRepository.save(user);
            throw new RuntimeException("CAPTCHA_REQUIRED");
        }

        // 2. Hồi 10% năng lượng khi bắt đầu trận
        int regen = (int)(player.getMaxEnergy() * 0.1);
        player.setEnergy(Math.min(player.getMaxEnergy(), player.getEnergy() + regen));
        characterRepository.save(player);

        // 3. Tìm quái theo cấp độ
        // FIX: Sửa player.getLevel() -> player.getLv()
        int minLv = Math.max(1, player.getLv() - 2);
        int maxLv = player.getLv() + 2;

        // FIX: Giả định enemyRepository có findByLevelRange
        List<Enemy> enemies = enemyRepository.findByLevelRange(minLv, maxLv);
        if (enemies.isEmpty()) enemies = enemyRepository.findAll();

        Enemy enemy = enemies.get(random.nextInt(enemies.size()));

        // 4. Khởi tạo kết quả trận đấu
        BattleResult result = new BattleResult();
        result.setEnemy(enemy);
        result.setPlayerHp(player.getHp());
        result.setPlayerMaxHp(player.getMaxHp());
        result.setEnemyHp(enemy.getHp());
        result.setEnemyMaxHp(enemy.getHp());
        result.setStatus("ONGOING");
        result.getCombatLog().add("⚔️ Gặp " + enemy.getName() + " (Lv." + enemy.getLevel() + ")!");

        return result;
    }

    @Transactional
    public BattleResult processTurn(Integer enemyId, Integer currentEnemyHp, boolean isParried, String attackType) {
        if (attackType == null) attackType = "normal";
        return executeTurn(enemyId, currentEnemyHp, null, isParried, attackType);
    }

    @Transactional
    public BattleResult useSkill(Integer enemyId, Integer currentEnemyHp, Integer skillId) {
        Skill skill = skillRepository.findById(skillId).orElseThrow(() -> new RuntimeException("Skill 404"));
        return executeTurn(enemyId, currentEnemyHp, skill, false, "skill");
    }

    // Hàm xử lý chính cho 1 lượt đánh
    private BattleResult executeTurn(Integer enemyId, Integer currentEnemyHp, Skill skill, boolean isParried, String attackType) {
        Character player = characterService.getMyCharacter();
        Enemy enemy = enemyRepository.findById(enemyId).orElseThrow();

        BattleResult result = new BattleResult();
        result.setEnemy(enemy);
        result.setPlayerMaxHp(player.getMaxHp());
        result.setEnemyMaxHp(enemy.getHp());

        // --- 1. LƯỢT CỦA NGƯỜI CHƠI ---
        int damageToEnemy = 0;
        double damageMultiplier = 1.0;

        // Xử lý loại đòn đánh
        if ("strong".equals(attackType)) {
            // FIX: Sử dụng player.getEnergy()
            if (player.getEnergy() >= 5) {
                player.setEnergy(player.getEnergy() - 5);
                damageMultiplier = 2.5;
                result.getCombatLog().add("⚡ Sấm Sét (-5 Energy)!");
            } else {
                result.getCombatLog().add("⚠️ Thiếu Energy! Đánh thường.");
            }
        }

        if (skill != null) {
            // Xử lý Skill (nếu có)
            if (player.getEnergy() < skill.getManaCost()) {
                throw new RuntimeException("Không đủ Mana!");
            }
            player.setEnergy(player.getEnergy() - skill.getManaCost());

            if ("HEAL".equals(skill.getType())) {
                // FIX: Sử dụng player.getBaseAtk()
                int healAmount = skill.getPower() + (int)(player.getBaseAtk() * 0.5);
                player.setHp(Math.min(player.getMaxHp(), player.getHp() + healAmount));
                result.getCombatLog().add("✨ " + skill.getName() + " hồi " + healAmount + " HP.");
            } else {
                // FIX: Sử dụng player.getBaseAtk()
                int baseDmg = skill.getPower() + player.getBaseAtk();
                damageToEnemy = Math.max(1, baseDmg - enemy.getDef());
                result.getCombatLog().add("🔥 " + skill.getName() + " gây " + damageToEnemy + " dmg!");
            }
        } else {
            // Đánh thường hoặc Đánh mạnh
            // FIX: Sử dụng player.getBaseAtk()
            int baseDmg = (int) (player.getBaseAtk() * (0.9 + random.nextDouble() * 0.2) * damageMultiplier);
            damageToEnemy = Math.max(1, baseDmg - enemy.getDef());

            // Tính chí mạng
            // FIX: Sử dụng player.getBaseCritRate() và player.getBaseCritDmg()
            if (random.nextInt(100) < player.getBaseCritRate()) {
                damageToEnemy = (int) (damageToEnemy * (player.getBaseCritDmg() / 100.0));
                result.getCombatLog().add("💥 CHÍ MẠNG! " + damageToEnemy + " dmg!");
            } else {
                if ("normal".equals(attackType)) result.getCombatLog().add("🗡️ Bạn chém " + damageToEnemy + " dmg.");
                if ("strong".equals(attackType) && damageMultiplier > 1.0) result.getCombatLog().add("💥 Đòn mạnh gây " + damageToEnemy + " dmg!");
            }
        }

        // Trừ máu quái
        int newEnemyHp = Math.max(0, currentEnemyHp - damageToEnemy);

        // Kiểm tra nếu quái chết ngay sau đòn đánh của người chơi
        if (newEnemyHp <= 0) {
            result.setStatus("VICTORY");
            result.getCombatLog().add("💀 " + enemy.getName() + " bị hạ gục!");
            grantReward(player, enemy, result);
            questService.updateProgress(player.getUser(), "KILL_ENEMY", 1);

            result.setPlayerHp(player.getHp());
            result.setEnemyHp(0);
            return result;
        }

        // --- 2. LƯỢT CỦA QUÁI VẬT (Tính cả Parry) ---
        int eDmg = (int) (enemy.getAtk() * (0.9 + random.nextDouble() * 0.2));
        int eNetDmg;

        if (isParried) {
            // Parry thành công: Người chơi không mất máu + Phản đòn
            eNetDmg = 0;
            result.getCombatLog().add("🛡️ PARRY THÀNH CÔNG!");

            // FIX: Sử dụng player.getBaseAtk()
            int counterDmg = (int)(player.getBaseAtk() * 0.75);
            newEnemyHp = Math.max(0, newEnemyHp - counterDmg);
            result.getCombatLog().add("⚔️ Phản đòn " + counterDmg + " dmg!");

            // Kiểm tra nếu quái chết do phản đòn
            if (newEnemyHp <= 0) {
                result.setStatus("VICTORY");
                result.getCombatLog().add("💀 Quái bị phản đòn chết!");
                grantReward(player, enemy, result);

                result.setPlayerHp(player.getHp());
                result.setEnemyHp(0);
                return result;
            }
        } else {
            // Không đỡ được: Trừ máu người chơi
            // FIX: Sử dụng player.getBaseDef()
            eNetDmg = Math.max(1, eDmg - player.getBaseDef());
            player.setHp(player.getHp() - eNetDmg);
            result.getCombatLog().add("👾 Quái đánh " + eNetDmg + " dmg!");
        }

        // Kiểm tra người chơi có chết không
        if (player.getHp() <= 0) {
            player.setHp(0);
            result.setStatus("DEFEAT");
            result.getCombatLog().add("☠️ Bạn đã gục ngã...");
        } else {
            result.setStatus("ONGOING");
        }

        // Lưu trạng thái và trả về kết quả
        characterRepository.save(player);
        result.setPlayerHp(player.getHp());
        result.setEnemyHp(newEnemyHp);

        return result;
    }

    // Hàm trao thưởng khi thắng
    private void grantReward(Character player, Enemy enemy, BattleResult result) {
        // 1. Kinh nghiệm
        player.setExp(player.getExp() + enemy.getExpReward());
        result.setExpEarned(enemy.getExpReward());

        // FIX: Sửa player.getLevel() -> player.getLv()
        long expNeed = player.getLv() * 100L;

        // Logic Level Up
        if (player.getExp() >= expNeed) {
            // FIX: Sửa setLevel -> setLv, setAtk -> setBaseAtk, setDef -> setBaseDef
            player.setLv(player.getLv() + 1);
            player.setExp(player.getExp() - (int) expNeed);

            // Tăng chỉ số
            player.setMaxHp(player.getMaxHp() + 20);
            player.setHp(player.getMaxHp());
            player.setBaseAtk(player.getBaseAtk() + 2);
            player.setBaseDef(player.getBaseDef() + 1);
            player.setEnergy(player.getMaxEnergy());

            result.getCombatLog().add("🆙 LÊN CẤP " + player.getLv() + "!");
            result.setLevelUp(true);
        }

        // 2. Vàng
        Wallet wallet = player.getUser().getWallet();
        wallet.setGold(wallet.getGold().add(new BigDecimal(enemy.getGoldReward())));
        result.setGoldEarned(enemy.getGoldReward());

        // 3. Drop Item (Tỷ lệ 20%)
        if (random.nextInt(100) < 20) {
            List<Item> items = itemRepository.findAll();
            if (!items.isEmpty()) {
                Item droppedItem = items.get(random.nextInt(items.size()));
                UserItem ui = new UserItem();
                ui.setUser(player.getUser());
                ui.setItem(droppedItem);
                ui.setQuantity(1);
                ui.setIsEquipped(false);
                ui.setEnhanceLevel(0);
                userItemRepository.save(ui);
                result.getCombatLog().add("🎁 NHẶT: " + droppedItem.getName());
            }
        }

        // Lưu thay đổi
        characterRepository.save(player);
        walletRepository.save(wallet);
    }

    public List<Skill> getAllSkills() { return skillRepository.findAll(); }
}