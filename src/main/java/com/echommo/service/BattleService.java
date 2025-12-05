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

<<<<<<< HEAD
//@Service
//public class BattleService {
//
//    @Autowired private CharacterService characterService;
//    @Autowired private EnemyRepository enemyRepository;
//    @Autowired private CharacterRepository characterRepository;
//    @Autowired private WalletRepository walletRepository;
//    @Autowired private SkillRepository skillRepository;
//    @Autowired private QuestService questService;
//    @Autowired private ItemRepository itemRepository;
//    @Autowired private UserItemRepository userItemRepository;
//    @Autowired private CaptchaService captchaService;
//    @Autowired private UserRepository userRepository;
//
//    private final Random random = new Random();
//
//    public BattleResult startBattle() {
//        Character player = characterService.getMyCharacter();
//        User user = player.getUser();
//
//        // 1. Check Captcha
//        captchaService.checkLockStatus(user);
//        if (Boolean.TRUE.equals(user.getIsCaptchaLocked())) {
//            throw new RuntimeException("CAPTCHA_REQUIRED");
//        }
//        // Tỷ lệ 10% hiện Captcha
//        if (random.nextInt(100) < 1) {
//            user.setIsCaptchaLocked(true);
//            userRepository.save(user);
//            throw new RuntimeException("CAPTCHA_REQUIRED");
//        }
//
//        // 2. Hồi 10% năng lượng
//        int regen = (int)(player.getMaxEnergy() * 0.1);
//        player.setEnergy(Math.min(player.getMaxEnergy(), player.getEnergy() + regen));
//        characterRepository.save(player);
//
//        // 3. Tìm quái
//        int minLv = Math.max(1, player.getLevel() - 2);
//        int maxLv = player.getLevel() + 2;
//        List<Enemy> enemies = enemyRepository.findByLevelRange(minLv, maxLv);
//        if (enemies.isEmpty()) enemies = enemyRepository.findAll();
//
//        Enemy enemy = enemies.get(random.nextInt(enemies.size()));
//
//        BattleResult result = new BattleResult();
//        result.setEnemy(enemy);
//        result.setPlayerHp(player.getHp());
//        result.setPlayerMaxHp(player.getMaxHp());
//        result.setEnemyHp(enemy.getHp());
//        result.setEnemyMaxHp(enemy.getHp());
//        result.setStatus("ONGOING");
//        result.getCombatLog().add("⚔️ Gặp " + enemy.getName() + " (Lv." + enemy.getLevel() + ")!");
//
//        return result;
//    }
//
//    @Transactional
//    public BattleResult processTurn(Integer enemyId, Integer currentEnemyHp, boolean isParried, String attackType) {
//        return executeTurn(enemyId, currentEnemyHp, null, isParried, attackType);
//    }
//
//    @Transactional
//    public BattleResult useSkill(Integer enemyId, Integer currentEnemyHp, Integer skillId) {
//        Skill skill = skillRepository.findById(skillId).orElseThrow(() -> new RuntimeException("Skill 404"));
//        return executeTurn(enemyId, currentEnemyHp, skill, false, "skill");
//    }
//
//    private BattleResult executeTurn(Integer enemyId, Integer currentEnemyHp, Skill skill, boolean isParried, String attackType) {
//        Character player = characterService.getMyCharacter();
//        Enemy enemy = enemyRepository.findById(enemyId).orElseThrow();
//
//        BattleResult result = new BattleResult();
//        result.setEnemy(enemy);
//        result.setPlayerMaxHp(player.getMaxHp());
//        result.setEnemyMaxHp(enemy.getHp());
//
//        // --- PLAYER ATTACK ---
//        int damageToEnemy = 0;
//        double damageMultiplier = 1.0;
//
//        if ("strong".equals(attackType)) {
//            if (player.getEnergy() >= 5) {
//                player.setEnergy(player.getEnergy() - 5);
//                damageMultiplier = 2.5;
//                result.getCombatLog().add("⚡ Sấm Sét (-5 Energy)!");
//            } else {
//                result.getCombatLog().add("⚠️ Thiếu Energy! Đánh thường.");
//            }
//        }
//
//        if (skill != null) {
//            if (player.getEnergy() < skill.getManaCost()) {
//                throw new RuntimeException("Không đủ Mana!");
//            }
//            player.setEnergy(player.getEnergy() - skill.getManaCost());
//
//            if ("HEAL".equals(skill.getType())) {
//                int healAmount = skill.getPower() + (int)(player.getAtk() * 0.5);
//                player.setHp(Math.min(player.getMaxHp(), player.getHp() + healAmount));
//                result.getCombatLog().add("✨ " + skill.getName() + " hồi " + healAmount + " HP.");
//            } else {
//                int baseDmg = skill.getPower() + player.getAtk();
//                damageToEnemy = Math.max(1, baseDmg - enemy.getDef());
//                result.getCombatLog().add("🔥 " + skill.getName() + " gây " + damageToEnemy + " dmg!");
//            }
//        } else {
//            int baseDmg = (int) (player.getAtk() * (0.9 + random.nextDouble() * 0.2) * damageMultiplier);
//            damageToEnemy = Math.max(1, baseDmg - enemy.getDef());
//
//            if (random.nextInt(100) < player.getCritRate()) {
//                damageToEnemy = (int) (damageToEnemy * (player.getCritDmg() / 100.0));
//                result.getCombatLog().add("💥 CHÍ MẠNG! " + damageToEnemy + " dmg!");
//            } else {
//                if ("normal".equals(attackType)) result.getCombatLog().add("🗡️ Bạn chém " + damageToEnemy + " dmg.");
//            }
//        }
//
//        int newEnemyHp = Math.max(0, currentEnemyHp - damageToEnemy);
//
//        if (newEnemyHp <= 0) {
//            result.setStatus("VICTORY");
//            result.getCombatLog().add("💀 " + enemy.getName() + " bị hạ gục!");
//            grantReward(player, enemy, result);
//            questService.updateProgress(player.getUser(), "KILL_ENEMY", 1);
//            result.setPlayerHp(player.getHp());
//            result.setEnemyHp(0);
//            return result;
//        }
//
//        // --- ENEMY ATTACK ---
//        int eDmg = (int) (enemy.getAtk() * (0.9 + random.nextDouble() * 0.2));
//        int eNetDmg;
//
//        if (isParried) {
//            eNetDmg = 0;
//            result.getCombatLog().add("🛡️ PARRY THÀNH CÔNG!");
//            int counterDmg = (int)(player.getAtk() * 0.5);
//            newEnemyHp = Math.max(0, newEnemyHp - counterDmg);
//            result.getCombatLog().add("⚔️ Phản đòn " + counterDmg + " dmg!");
//            if (newEnemyHp <= 0) {
//                result.setStatus("VICTORY");
//                result.getCombatLog().add("💀 Quái bị phản đòn chết!");
//                grantReward(player, enemy, result);
//                result.setPlayerHp(player.getHp());
//                result.setEnemyHp(0);
//                return result;
//            }
//        } else {
//            eNetDmg = Math.max(1, eDmg - player.getDef());
//            player.setHp(player.getHp() - eNetDmg);
//            result.getCombatLog().add("👾 Quái đánh " + eNetDmg + " dmg!");
//        }
//
//        if (player.getHp() <= 0) {
//            player.setHp(0);
//            result.setStatus("DEFEAT");
//            result.getCombatLog().add("☠️ Bạn đã gục ngã...");
//        } else {
//            result.setStatus("ONGOING");
//        }
//
//        characterRepository.save(player);
//        result.setPlayerHp(player.getHp());
//        result.setEnemyHp(newEnemyHp);
//        return result;
//    }
//
//    private void grantReward(Character player, Enemy enemy, BattleResult result) {
//        player.setCurrentExp(player.getCurrentExp() + enemy.getExpReward());
//        result.setExpEarned(enemy.getExpReward());
//        long expNeed = player.getLevel() * 100L;
//        if (player.getCurrentExp() >= expNeed) {
//            player.setLevel(player.getLevel() + 1);
//            player.setCurrentExp(player.getCurrentExp() - expNeed);
//            player.setMaxHp(player.getMaxHp() + 20);
//            player.setHp(player.getMaxHp());
//            player.setAtk(player.getAtk() + 2);
//            player.setDef(player.getDef() + 1);
//            player.setEnergy(player.getMaxEnergy());
//            result.getCombatLog().add("🆙 LÊN CẤP " + player.getLevel() + "!");
//        }
//
//        Wallet wallet = player.getUser().getWallet();
//        wallet.setGold(wallet.getGold().add(new BigDecimal(enemy.getGoldReward())));
//        result.setGoldEarned(enemy.getGoldReward());
//
//        if (random.nextInt(100) < 20) {
//            List<Item> items = itemRepository.findAll();
//            if (!items.isEmpty()) {
//                Item droppedItem = items.get(random.nextInt(items.size()));
//                UserItem ui = new UserItem();
//                ui.setUser(player.getUser());
//                ui.setItem(droppedItem);
//                ui.setQuantity(1);
//                ui.setIsEquipped(false);
//                ui.setEnhanceLevel(0);
//                userItemRepository.save(ui);
//                result.getCombatLog().add("🎁 NHẶT: " + droppedItem.getName());
//            }
//        }
//
//        characterRepository.save(player);
//        walletRepository.save(wallet);
//    }
//
//    public List<Skill> getAllSkills() { return skillRepository.findAll(); }
//}

=======
>>>>>>> 31f4b17a4f519d2a38168af40e596afb5316f91a
@Service
public class BattleService {

    @Autowired private CharacterService characterService;
    @Autowired private EnemyRepository enemyRepository;
    @Autowired private CharacterRepository characterRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private SkillRepository skillRepository;
    @Autowired private QuestService questService;
    @Autowired private ItemRepository itemRepository;
    @Autowired private UserItemRepository userItemRepository;
    @Autowired private CaptchaService captchaService;
    @Autowired private UserRepository userRepository;

    private final Random random = new Random();

    public BattleResult startBattle() {
        Character player = characterService.getMyCharacter();
        User user = player.getUser();

        // 1. Check Captcha
        captchaService.checkLockStatus(user);
        if (Boolean.TRUE.equals(user.getIsCaptchaLocked())) {
            throw new RuntimeException("CAPTCHA_REQUIRED");
        }
<<<<<<< HEAD

        // Giảm tỷ lệ Captcha xuống 1% để trải nghiệm mượt hơn
=======
        // Tỷ lệ 10% hiện Captcha
>>>>>>> 31f4b17a4f519d2a38168af40e596afb5316f91a
        if (random.nextInt(100) < 1) {
            user.setIsCaptchaLocked(true);
            userRepository.save(user);
            throw new RuntimeException("CAPTCHA_REQUIRED");
        }

<<<<<<< HEAD
        // 2. Hồi 10% năng lượng khi bắt đầu trận
=======
        // 2. Hồi 10% năng lượng
>>>>>>> 31f4b17a4f519d2a38168af40e596afb5316f91a
        int regen = (int)(player.getMaxEnergy() * 0.1);
        player.setEnergy(Math.min(player.getMaxEnergy(), player.getEnergy() + regen));
        characterRepository.save(player);

        // 3. Tìm quái
        int minLv = Math.max(1, player.getLevel() - 2);
        int maxLv = player.getLevel() + 2;
        List<Enemy> enemies = enemyRepository.findByLevelRange(minLv, maxLv);
        if (enemies.isEmpty()) enemies = enemyRepository.findAll();

        Enemy enemy = enemies.get(random.nextInt(enemies.size()));

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
        return executeTurn(enemyId, currentEnemyHp, null, isParried, attackType);
    }

    @Transactional
    public BattleResult useSkill(Integer enemyId, Integer currentEnemyHp, Integer skillId) {
        Skill skill = skillRepository.findById(skillId).orElseThrow(() -> new RuntimeException("Skill 404"));
        return executeTurn(enemyId, currentEnemyHp, skill, false, "skill");
    }

    private BattleResult executeTurn(Integer enemyId, Integer currentEnemyHp, Skill skill, boolean isParried, String attackType) {
        Character player = characterService.getMyCharacter();
        Enemy enemy = enemyRepository.findById(enemyId).orElseThrow();

        BattleResult result = new BattleResult();
        result.setEnemy(enemy);
        result.setPlayerMaxHp(player.getMaxHp());
        result.setEnemyMaxHp(enemy.getHp());

<<<<<<< HEAD
        // --- 1. PLAYER ATTACK ---
        int damageToEnemy = 0;
        double damageMultiplier = 1.0;

        // Logic đánh mạnh (tốn Energy)
        if ("strong".equals(attackType)) {
            if (player.getEnergy() >= 5) {
                player.setEnergy(player.getEnergy() - 5);
                damageMultiplier = 2.5; // Sát thương x2.5
=======
        // --- PLAYER ATTACK ---
        int damageToEnemy = 0;
        double damageMultiplier = 1.0;

        if ("strong".equals(attackType)) {
            if (player.getEnergy() >= 5) {
                player.setEnergy(player.getEnergy() - 5);
                damageMultiplier = 2.5;
>>>>>>> 31f4b17a4f519d2a38168af40e596afb5316f91a
                result.getCombatLog().add("⚡ Sấm Sét (-5 Energy)!");
            } else {
                result.getCombatLog().add("⚠️ Thiếu Energy! Đánh thường.");
            }
        }

        if (skill != null) {
            if (player.getEnergy() < skill.getManaCost()) {
                throw new RuntimeException("Không đủ Mana!");
            }
            player.setEnergy(player.getEnergy() - skill.getManaCost());

            if ("HEAL".equals(skill.getType())) {
                int healAmount = skill.getPower() + (int)(player.getAtk() * 0.5);
                player.setHp(Math.min(player.getMaxHp(), player.getHp() + healAmount));
                result.getCombatLog().add("✨ " + skill.getName() + " hồi " + healAmount + " HP.");
            } else {
                int baseDmg = skill.getPower() + player.getAtk();
                damageToEnemy = Math.max(1, baseDmg - enemy.getDef());
                result.getCombatLog().add("🔥 " + skill.getName() + " gây " + damageToEnemy + " dmg!");
            }
        } else {
<<<<<<< HEAD
            // Đánh thường / Mạnh
=======
>>>>>>> 31f4b17a4f519d2a38168af40e596afb5316f91a
            int baseDmg = (int) (player.getAtk() * (0.9 + random.nextDouble() * 0.2) * damageMultiplier);
            damageToEnemy = Math.max(1, baseDmg - enemy.getDef());

            if (random.nextInt(100) < player.getCritRate()) {
                damageToEnemy = (int) (damageToEnemy * (player.getCritDmg() / 100.0));
                result.getCombatLog().add("💥 CHÍ MẠNG! " + damageToEnemy + " dmg!");
            } else {
                if ("normal".equals(attackType)) result.getCombatLog().add("🗡️ Bạn chém " + damageToEnemy + " dmg.");
<<<<<<< HEAD
                if ("strong".equals(attackType) && damageMultiplier > 1.0) result.getCombatLog().add("💥 Đòn mạnh gây " + damageToEnemy + " dmg!");
=======
>>>>>>> 31f4b17a4f519d2a38168af40e596afb5316f91a
            }
        }

        int newEnemyHp = Math.max(0, currentEnemyHp - damageToEnemy);

<<<<<<< HEAD
        // Check thắng ngay sau đòn đánh của player
=======
>>>>>>> 31f4b17a4f519d2a38168af40e596afb5316f91a
        if (newEnemyHp <= 0) {
            result.setStatus("VICTORY");
            result.getCombatLog().add("💀 " + enemy.getName() + " bị hạ gục!");
            grantReward(player, enemy, result);
            questService.updateProgress(player.getUser(), "KILL_ENEMY", 1);
            result.setPlayerHp(player.getHp());
            result.setEnemyHp(0);
            return result;
        }

<<<<<<< HEAD
        // --- 2. ENEMY ATTACK (Có tính Parry) ---
=======
        // --- ENEMY ATTACK ---
>>>>>>> 31f4b17a4f519d2a38168af40e596afb5316f91a
        int eDmg = (int) (enemy.getAtk() * (0.9 + random.nextDouble() * 0.2));
        int eNetDmg;

        if (isParried) {
<<<<<<< HEAD
            // Parry thành công: Không mất máu + Phản đòn
            eNetDmg = 0;
            result.getCombatLog().add("🛡️ PARRY THÀNH CÔNG!");

            int counterDmg = (int)(player.getAtk() * 0.75); // Phản 75% sát thương
            newEnemyHp = Math.max(0, newEnemyHp - counterDmg);
            result.getCombatLog().add("⚔️ Phản đòn " + counterDmg + " dmg!");

=======
            eNetDmg = 0;
            result.getCombatLog().add("🛡️ PARRY THÀNH CÔNG!");
            int counterDmg = (int)(player.getAtk() * 0.5);
            newEnemyHp = Math.max(0, newEnemyHp - counterDmg);
            result.getCombatLog().add("⚔️ Phản đòn " + counterDmg + " dmg!");
>>>>>>> 31f4b17a4f519d2a38168af40e596afb5316f91a
            if (newEnemyHp <= 0) {
                result.setStatus("VICTORY");
                result.getCombatLog().add("💀 Quái bị phản đòn chết!");
                grantReward(player, enemy, result);
                result.setPlayerHp(player.getHp());
                result.setEnemyHp(0);
                return result;
            }
        } else {
<<<<<<< HEAD
            // Không đỡ được
=======
>>>>>>> 31f4b17a4f519d2a38168af40e596afb5316f91a
            eNetDmg = Math.max(1, eDmg - player.getDef());
            player.setHp(player.getHp() - eNetDmg);
            result.getCombatLog().add("👾 Quái đánh " + eNetDmg + " dmg!");
        }

        if (player.getHp() <= 0) {
            player.setHp(0);
            result.setStatus("DEFEAT");
            result.getCombatLog().add("☠️ Bạn đã gục ngã...");
        } else {
            result.setStatus("ONGOING");
        }

        characterRepository.save(player);
        result.setPlayerHp(player.getHp());
        result.setEnemyHp(newEnemyHp);
<<<<<<< HEAD

=======
>>>>>>> 31f4b17a4f519d2a38168af40e596afb5316f91a
        return result;
    }

    private void grantReward(Character player, Enemy enemy, BattleResult result) {
        player.setCurrentExp(player.getCurrentExp() + enemy.getExpReward());
        result.setExpEarned(enemy.getExpReward());
<<<<<<< HEAD

        long expNeed = player.getLevel() * 100L;
        // Logic Level Up
        if (player.getCurrentExp() >= expNeed) {
            player.setLevel(player.getLevel() + 1);
            player.setCurrentExp(player.getCurrentExp() - expNeed);

            player.setMaxHp(player.getMaxHp() + 20);
            player.setHp(player.getMaxHp()); // Hồi đầy HP
            player.setAtk(player.getAtk() + 2);
            player.setDef(player.getDef() + 1);
            player.setEnergy(player.getMaxEnergy()); // Hồi đầy Energy

            result.getCombatLog().add("🆙 LÊN CẤP " + player.getLevel() + "!");
            result.setLevelUp(true); // Set cờ cho Frontend
=======
        long expNeed = player.getLevel() * 100L;
        if (player.getCurrentExp() >= expNeed) {
            player.setLevel(player.getLevel() + 1);
            player.setCurrentExp(player.getCurrentExp() - expNeed);
            player.setMaxHp(player.getMaxHp() + 20);
            player.setHp(player.getMaxHp());
            player.setAtk(player.getAtk() + 2);
            player.setDef(player.getDef() + 1);
            player.setEnergy(player.getMaxEnergy());
            result.getCombatLog().add("🆙 LÊN CẤP " + player.getLevel() + "!");
>>>>>>> 31f4b17a4f519d2a38168af40e596afb5316f91a
        }

        Wallet wallet = player.getUser().getWallet();
        wallet.setGold(wallet.getGold().add(new BigDecimal(enemy.getGoldReward())));
        result.setGoldEarned(enemy.getGoldReward());

<<<<<<< HEAD
        // Drop Item tỷ lệ 20%
=======
>>>>>>> 31f4b17a4f519d2a38168af40e596afb5316f91a
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

        characterRepository.save(player);
        walletRepository.save(wallet);
    }

    public List<Skill> getAllSkills() { return skillRepository.findAll(); }
}