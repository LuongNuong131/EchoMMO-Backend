package com.echommo.service;

import com.echommo.dto.BattleResult;
import com.echommo.entity.Character;
import com.echommo.entity.Enemy;
import com.echommo.entity.Skill;
import com.echommo.entity.Wallet;
import com.echommo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@Transactional
public class BattleService {
    @Autowired private CharacterRepository charRepo;
    @Autowired private EnemyRepository enemyRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private WalletRepository walletRepo;
    @Autowired private SkillRepository skillRepository;

    private final Random random = new Random();

    public BattleResult startBattle() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Character player = userRepo.findByUsername(username).get().getCharacter();

        // Random quái
        List<Enemy> enemies = enemyRepo.findAll();
        Enemy enemy = enemies.get(random.nextInt(enemies.size()));

        BattleResult res = new BattleResult();
        res.setEnemy(enemy);
        res.setPlayerHp(player.getHp());
        res.setPlayerMaxHp(player.getMaxHp());
        res.setEnemyHp(enemy.getHp());
        res.setEnemyMaxHp(enemy.getHp());
        res.setStatus("ONGOING");
        res.setCombatLog(new ArrayList<>());
        res.getCombatLog().add("⚔️ Gặp " + enemy.getName() + "! (Tốc độ: " + (10 + enemy.getLevel() * 2) + ")");

        return res;
    }

    // LOGIC ĐÁNH TỰ ĐỘNG (1 LƯỢT)
    public BattleResult attackEnemy(Map<String, Object> payload) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Character player = userRepo.findByUsername(username).get().getCharacter();

        Integer enemyId = (Integer) payload.get("enemyId");
        Integer currentEnemyHp = (Integer) payload.get("enemyHp");
        Boolean isBuffed = (Boolean) payload.get("isBuffed");
        if (isBuffed == null) isBuffed = false;

        Enemy enemy = enemyRepo.findById(enemyId).orElseThrow();
        BattleResult result = new BattleResult();
        result.setEnemy(enemy);
        result.setPlayerMaxHp(player.getMaxHp());
        result.setEnemyMaxHp(enemy.getHp());
        List<String> logs = new ArrayList<>();

        // 1. SO SÁNH TỐC ĐỘ
        int enemySpeed = 10 + enemy.getLevel() * 2;
        int playerSpeed = player.getBaseSpeed();
        boolean playerFirst = playerSpeed >= enemySpeed;

        // 2. XỬ LÝ BUFF TỤ LỰC (Tiêu hao Energy)
        double dmgMultiplier = 1.0;
        if (isBuffed) {
            if (player.getEnergy() >= 5) {
                player.setEnergy(player.getEnergy() - 5);
                dmgMultiplier = 1.5; // Tăng 50% sát thương
                logs.add("🔥 Tụ lực thành công! Sát thương tăng mạnh.");
            } else {
                logs.add("⚠️ Không đủ nội lực để tụ lực!");
            }
        }

        // 3. GIAO CHIẾN
        if (playerFirst) {
            // Người đánh trước
            currentEnemyHp = doPlayerAttack(player, enemy, currentEnemyHp, dmgMultiplier, logs);
            if (currentEnemyHp > 0) {
                doEnemyAttack(player, enemy, logs); // Quái phản công
            }
        } else {
            // Quái đánh trước
            logs.add("⚡ Quái nhanh hơn! Nó tấn công trước.");
            doEnemyAttack(player, enemy, logs);
            if (player.getHp() > 0) {
                currentEnemyHp = doPlayerAttack(player, enemy, currentEnemyHp, dmgMultiplier, logs); // Người phản công
            }
        }

        // 4. KẾT QUẢ
        if (player.getHp() <= 0) {
            result.setStatus("DEFEAT");
            player.setHp(0);
            logs.add("💀 Bạn đã gục ngã...");
        } else if (currentEnemyHp <= 0) {
            result.setStatus("VICTORY");
            currentEnemyHp = 0;

            int gold = enemy.getGoldReward();
            int exp = enemy.getExpReward();
            player.setExp(player.getExp() + exp);

            Wallet w = player.getUser().getWallet();
            w.setGold(w.getGold().add(BigDecimal.valueOf(gold)));
            walletRepo.save(w);

            result.setExpEarned(exp);
            result.setGoldEarned(gold);
            logs.add("🏆 Chiến thắng! Nhận " + exp + " Exp, " + gold + " Vàng.");

            checkLevelUp(player, logs);
        } else {
            result.setStatus("ONGOING");
        }

        // [FIX BUG] Lưu máu nhân vật
        charRepo.save(player);

        result.setPlayerHp(player.getHp());
        result.setPlayerEnergy(player.getEnergy());
        result.setEnemyHp(currentEnemyHp);
        result.setCombatLog(logs);

        return result;
    }

    private int doPlayerAttack(Character p, Enemy e, int eHp, double mul, List<String> logs) {
        int baseDmg = p.getBaseAtk();
        // Crit logic
        boolean isCrit = random.nextInt(100) < p.getBaseCritRate();
        if (isCrit) {
            baseDmg = (int) (baseDmg * (p.getBaseCritDmg() / 100.0));
        }

        int dmg = (int) (Math.max(1, baseDmg - e.getDef()) * mul);
        eHp -= dmg;

        String msg = isCrit ? "💥 BẠO KÍCH! Gây " + dmg + " sát thương." : "⚔️ Bạn gây " + dmg + " sát thương.";
        logs.add(msg);
        return Math.max(0, eHp);
    }

    private void doEnemyAttack(Character p, Enemy e, List<String> logs) {
        int dmg = Math.max(1, e.getAtk() - p.getBaseDef());
        p.setHp(Math.max(0, p.getHp() - dmg)); // Trừ máu nhân vật
        logs.add("👾 " + e.getName() + " đánh bạn mất " + dmg + " máu!");
    }

    private void checkLevelUp(Character c, List<String> logs) {
        long req = 100L * (long) Math.pow(c.getLv(), 2);
        if (c.getExp() >= req) {
            c.setExp((int)(c.getExp() - req));
            c.setLv(c.getLv() + 1);
            c.setMaxHp(c.getMaxHp() + 20);
            c.setHp(c.getMaxHp());
            c.setBaseAtk(c.getBaseAtk() + 5);
            c.setBaseDef(c.getBaseDef() + 2);
            logs.add("🌟 CHÚC MỪNG! Đã thăng lên cấp " + c.getLv());
        }
    }

    public List<Skill> getAllSkills() {
        return skillRepository.findAll();
    }
}