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
        // [FIX] Auto create if null
        if (player == null) {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(username).orElseThrow();
            player = characterService.createDefaultCharacter(user);
        }

        // Captcha check
        captchaService.checkLockStatus(player.getUser());
        if (random.nextInt(100) < 1) {
            User u = player.getUser();
            u.setIsCaptchaLocked(true);
            userRepository.save(u);
            throw new RuntimeException("CAPTCHA_REQUIRED");
        }

        // Regen 10% energy
        int regen = (int)(player.getMaxEnergy() * 0.1);
        player.setEnergy(Math.min(player.getMaxEnergy(), player.getEnergy() + regen));
        characterRepository.save(player);

        // Tìm quái
        int minLv = Math.max(1, player.getLv() - 2);
        int maxLv = player.getLv() + 2;
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
        Character player = characterService.getMyCharacter(); // Đã có startBattle tạo rồi nên không cần check null ở đây
        Enemy enemy = enemyRepository.findById(enemyId).orElseThrow();
        BattleResult result = new BattleResult();
        result.setEnemy(enemy);
        result.setPlayerMaxHp(player.getMaxHp());
        result.setEnemyMaxHp(enemy.getHp());

        // 1. Check Energy
        int cost = "strong".equals(attackType) ? 5 : 2;
        if (player.getEnergy() < cost) {
            result.setStatus("ONGOING");
            result.setPlayerHp(player.getHp());
            result.setEnemyHp(currentEnemyHp);
            result.getCombatLog().add("⚠️ Không đủ Nội Lực!");
            return result;
        }
        player.setEnergy(player.getEnergy() - cost);

        // 2. Player đánh (Logic GameFi: Crit)
        double mul = "strong".equals(attackType) ? 2.0 : 1.0;
        int dmg = (int) (player.getBaseAtk() * (0.9 + random.nextDouble() * 0.2) * mul);

        if (random.nextInt(100) < player.getBaseCritRate()) {
            dmg = (int)(dmg * (player.getBaseCritDmg() / 100.0));
            result.getCombatLog().add("💥 BẠO KÍCH! Gây " + dmg + " sát thương!");
        } else {
            result.getCombatLog().add("🗡️ Bạn đánh gây " + dmg + " sát thương.");
        }

        int dmgToEnemy = Math.max(1, dmg - enemy.getDef());
        int newEnemyHp = Math.max(0, currentEnemyHp - dmgToEnemy);

        if (newEnemyHp <= 0) {
            handleVictory(player, enemy, result);
            result.setPlayerHp(player.getHp());
            result.setEnemyHp(0);
            return result;
        }

        // 3. Enemy đánh (Logic GameFi: Parry)
        if (isParried) {
            result.getCombatLog().add("🛡️ ĐỠ ĐÒN THÀNH CÔNG!");
            int counter = (int)(player.getBaseAtk() * 0.5);
            newEnemyHp = Math.max(0, newEnemyHp - counter);
            result.getCombatLog().add("⚔️ Phản kích " + counter + " dmg!");

            if (newEnemyHp <= 0) {
                handleVictory(player, enemy, result);
                result.setPlayerHp(player.getHp());
                result.setEnemyHp(0);
                return result;
            }
        } else {
            int eDmg = (int) (enemy.getAtk() * (0.9 + random.nextDouble() * 0.2));
            int take = Math.max(1, eDmg - player.getBaseDef());
            player.setHp(Math.max(0, player.getHp() - take));
            result.getCombatLog().add("👾 Bị đánh trúng " + take + " HP!");
        }

        if (player.getHp() <= 0) {
            result.setStatus("DEFEAT");
            result.getCombatLog().add("💀 Bạn đã trọng thương...");
            player.setHp(1); // Không chết, chỉ còn 1 máu
        } else {
            result.setStatus("ONGOING");
        }

        characterRepository.save(player);
        result.setPlayerHp(player.getHp());
        result.setEnemyHp(newEnemyHp);
        return result;
    }

    @Transactional
    public BattleResult useSkill(Integer enemyId, Integer currentEnemyHp, Integer skillId) {
        // Placeholder: Skill logic sẽ thêm sau
        return processTurn(enemyId, currentEnemyHp, false, "normal");
    }

    private void handleVictory(Character player, Enemy enemy, BattleResult result) {
        result.setStatus("VICTORY");
        result.getCombatLog().add("🏆 Chiến thắng " + enemy.getName());
        questService.updateProgress(player.getUser(), "KILL_ENEMY", 1);

        int exp = enemy.getExpReward();
        int gold = enemy.getGoldReward();
        player.setExp(player.getExp() + exp);
        result.setExpEarned(exp);
        result.setGoldEarned(gold);

        Wallet w = player.getUser().getWallet();
        w.setGold(w.getGold().add(BigDecimal.valueOf(gold)));
        walletRepository.save(w);

        long req = 100L * (long)Math.pow(player.getLv(), 2);
        if (player.getExp() >= req) {
            player.setExp(player.getExp() - (int)req);
            player.setLv(player.getLv() + 1);
            player.setMaxHp(player.getMaxHp() + 20);
            player.setHp(player.getMaxHp());
            player.setBaseAtk(player.getBaseAtk() + 2);
            player.setBaseDef(player.getBaseDef() + 1);
            player.setEnergy(player.getMaxEnergy());
            result.setLevelUp(true);
            result.getCombatLog().add("🌟 ĐỘT PHÁ CẢNH GIỚI! Cấp " + player.getLv());
        }

        if (random.nextInt(100) < 30) {
            List<Item> drops = itemRepository.findAll();
            if (!drops.isEmpty()) {
                Item drop = drops.get(random.nextInt(drops.size()));
                UserItem ui = new UserItem();
                ui.setUser(player.getUser());
                ui.setItem(drop);
                ui.setQuantity(1);
                userItemRepository.save(ui);
                result.getCombatLog().add("🎁 Nhặt được: " + drop.getName());
            }
        }
        characterRepository.save(player);
    }

    public List<Skill> getAllSkills() { return skillRepository.findAll(); }
}