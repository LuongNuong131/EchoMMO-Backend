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
    @Autowired private EnemyRepository enemyRepository;
    @Autowired private CharacterRepository characterRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private UserItemRepository userItemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CaptchaService captchaService;
    @Autowired private SkillRepository skillRepository;
    @Autowired private QuestService questService;

    private final Random random = new Random();

    public BattleResult startBattle() {
        Character player = characterService.getMyCharacter();
        User user = player.getUser();

        // 1. Check Captcha (Chống Auto - Tỷ lệ 1%)
        captchaService.checkLockStatus(user);
        if (Boolean.TRUE.equals(user.getIsCaptchaLocked())) {
            throw new RuntimeException("CAPTCHA_REQUIRED");
        }
        if (random.nextInt(100) < 1) {
            user.setIsCaptchaLocked(true);
            userRepository.save(user);
            throw new RuntimeException("CAPTCHA_REQUIRED");
        }

        // 2. Hồi 10% năng lượng khi bắt đầu trận
        int regen = (int)(player.getMaxEnergy() * 0.1);
        player.setEnergy(Math.min(player.getMaxEnergy(), player.getEnergy() + regen));
        characterRepository.save(player);

        // 3. Tìm quái theo cấp độ (Lv +/- 2)
        int minLv = Math.max(1, player.getLv() - 2);
        int maxLv = player.getLv() + 2;

        List<Enemy> enemies = enemyRepository.findByLevelRange(minLv, maxLv);
        if (enemies.isEmpty()) enemies = enemyRepository.findAll();

        Enemy enemy = enemies.get(random.nextInt(enemies.size()));

        // 4. Khởi tạo kết quả
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

    // --- CORE LOGIC ---
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

        // Tính năng lượng tiêu hao
        int energyCost = "strong".equals(attackType) ? 5 : 2;
        if (skill != null) energyCost = skill.getManaCost(); // Nếu dùng skill thì lấy cost của skill

        if (player.getEnergy() < energyCost) {
            result.setStatus("ONGOING");
            result.setPlayerHp(player.getHp());
            result.setEnemyHp(currentEnemyHp);
            result.getCombatLog().add("⚠️ Không đủ Thể Lực/Mana!");
            return result;
        }
        player.setEnergy(player.getEnergy() - energyCost);

        // Tính sát thương
        if (skill != null) {
            if ("HEAL".equals(skill.getType())) {
                int healAmount = skill.getPower() + (int)(player.getBaseAtk() * 0.5);
                player.setHp(Math.min(player.getMaxHp(), player.getHp() + healAmount));
                result.getCombatLog().add("✨ " + skill.getName() + " hồi " + healAmount + " HP.");
            } else {
                int baseDmg = skill.getPower() + player.getBaseAtk();
                damageToEnemy = Math.max(1, baseDmg - enemy.getDef());
                result.getCombatLog().add("🔥 " + skill.getName() + " gây " + damageToEnemy + " dmg!");
            }
        } else {
            // Đánh thường / Mạnh
            if ("strong".equals(attackType)) {
                damageMultiplier = 2.0;
                result.getCombatLog().add("⚡ Đòn mạnh (-5 Energy)!");
            }

            int baseDmg = (int) (player.getBaseAtk() * (0.9 + random.nextDouble() * 0.2) * damageMultiplier);

            // Crit Check
            if (random.nextInt(100) < player.getBaseCritRate()) {
                baseDmg = (int)(baseDmg * (player.getBaseCritDmg() / 100.0));
                result.getCombatLog().add("💥 CHÍ MẠNG! Gây " + baseDmg + " dmg!");
            } else {
                if("normal".equals(attackType)) result.getCombatLog().add("🗡️ Bạn đánh gây " + baseDmg + " dmg.");
            }

            damageToEnemy = Math.max(1, baseDmg - enemy.getDef());
        }

        int newEnemyHp = Math.max(0, currentEnemyHp - damageToEnemy);

        // Check Win ngay sau đòn đánh
        if (newEnemyHp <= 0) {
            handleVictory(player, enemy, result);
            result.setPlayerHp(player.getHp());
            result.setEnemyHp(0);
            return result;
        }

        // --- 2. LƯỢT CỦA QUÁI ---
        if (isParried) {
            result.getCombatLog().add("🛡️ PARRY THÀNH CÔNG! (Không mất máu)");
            int counterDmg = (int)(player.getBaseAtk() * 0.5);
            newEnemyHp = Math.max(0, newEnemyHp - counterDmg);
            result.getCombatLog().add("⚔️ Phản đòn gây " + counterDmg + " dmg!");

            if (newEnemyHp <= 0) {
                handleVictory(player, enemy, result);
                result.setPlayerHp(player.getHp());
                result.setEnemyHp(0);
                return result;
            }
        } else {
            int enemyDmg = (int) (enemy.getAtk() * (0.9 + random.nextDouble() * 0.2));
            int damageToPlayer = Math.max(1, enemyDmg - player.getBaseDef());
            player.setHp(Math.max(0, player.getHp() - damageToPlayer));
            result.getCombatLog().add("👾 Quái đánh bạn mất " + damageToPlayer + " HP!");
        }

        // Check Thua
        if (player.getHp() <= 0) {
            player.setHp(0);
            result.setStatus("DEFEAT");
            result.getCombatLog().add("💀 Bạn đã gục ngã...");
            // Phạt: Hồi sinh 50% HP
            player.setHp(player.getMaxHp() / 2);
        } else {
            result.setStatus("ONGOING");
        }

        characterRepository.save(player);
        result.setPlayerHp(player.getHp());
        result.setEnemyHp(newEnemyHp);

        return result;
    }

    private void handleVictory(Character player, Enemy enemy, BattleResult result) {
        result.setStatus("VICTORY");
        result.getCombatLog().add("🏆 Chiến thắng! Hạ gục " + enemy.getName());

        // Quest Progress
        questService.updateProgress(player.getUser(), "KILL_ENEMY", 1);

        // EXP
        int exp = enemy.getExpReward();
        player.setExp(player.getExp() + exp);
        result.setExpEarned(exp);

        // Level Up Logic
        long reqExp = 100L * (long)Math.pow(player.getLv(), 2);
        if (player.getExp() >= reqExp) {
            player.setExp(player.getExp() - (int)reqExp);
            player.setLv(player.getLv() + 1);
            player.setMaxHp(player.getMaxHp() + 20);
            player.setHp(player.getMaxHp());
            player.setBaseAtk(player.getBaseAtk() + 2);
            player.setBaseDef(player.getBaseDef() + 1);
            player.setEnergy(player.getMaxEnergy());

            result.setLevelUp(true);
            result.getCombatLog().add("🌟 LÊN CẤP ĐỘ " + player.getLv() + "!");
        }

        // Gold
        int gold = enemy.getGoldReward();
        Wallet w = player.getUser().getWallet();
        w.setGold(w.getGold().add(BigDecimal.valueOf(gold)));
        walletRepository.save(w);
        result.setGoldEarned(gold);

        // Drop Item (30%)
        if (random.nextInt(100) < 30) {
            List<Item> allItems = itemRepository.findAll();
            if (!allItems.isEmpty()) {
                Item drop = allItems.get(random.nextInt(allItems.size()));
                UserItem ui = new UserItem();
                ui.setUser(player.getUser());
                ui.setItem(drop);
                ui.setQuantity(1);
                ui.setIsEquipped(false);
                ui.setEnhanceLevel(0);
                userItemRepository.save(ui);
                result.getCombatLog().add("🎁 NHẶT ĐƯỢC: " + drop.getName());
            }
        }
        characterRepository.save(player);
    }

    public List<Skill> getAllSkills() { return skillRepository.findAll(); }
}