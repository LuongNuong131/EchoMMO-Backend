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
import java.util.*;

@Service
public class BattleService {

    @Autowired private CharacterRepository charRepo;
    @Autowired private EnemyRepository enemyRepo;
    @Autowired private WalletRepository walletRepo;
    @Autowired private ItemRepository itemRepo;
    @Autowired private UserItemRepository userItemRepo;
    @Autowired private UserRepository userRepo;

    private final Random random = new Random();
    private static final double DROP_RATE = 0.3; // 30% tỷ lệ rơi đồ

    public List<Skill> getAllSkills() {
        return new ArrayList<>();
    }

    // Logic tìm quái giữ nguyên
    @Transactional
    public BattleResult startBattle() {
        List<Enemy> enemies = enemyRepo.findAll();
        if (enemies.isEmpty()) {
            Enemy dummy = new Enemy();
            dummy.setEnemyId(1);
            dummy.setName("Hình Nhân Gỗ");
            dummy.setHp(100);
            dummy.setLevel(1);
            dummy.setAtk(5);
            dummy.setDef(0);
            dummy.setExpReward(10);
            dummy.setGoldReward(10);

            BattleResult res = new BattleResult();
            res.setEnemy(dummy);
            res.setEnemyHp(100);
            res.setEnemyMaxHp(100);
            res.setStatus("ONGOING");
            return res;
        }

        Enemy enemy = enemies.get(random.nextInt(enemies.size()));

        BattleResult result = new BattleResult();
        result.setEnemy(enemy);
        result.setEnemyHp(enemy.getHp());
        result.setEnemyMaxHp(enemy.getHp());

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByUsername(username).orElse(null);
        if (user != null) {
            Character c = charRepo.findByUser_UserId(user.getUserId()).orElse(null);
            if (c != null) {
                result.setPlayerHp(c.getHp());
                result.setPlayerMaxHp(c.getMaxHp());
                result.setPlayerEnergy(c.getEnergy());
            }
        }

        result.setStatus("ONGOING");
        result.setMessage("Gặp " + enemy.getName() + "! Chuẩn bị chiến đấu.");
        return result;
    }

    @Transactional
    public BattleResult attackEnemy(Map<String, Object> payload) {
        if (!payload.containsKey("enemyId")) {
            throw new RuntimeException("Thiếu enemyId");
        }
        Integer enemyId = Integer.parseInt(payload.get("enemyId").toString());
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Character character = charRepo.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Chưa tạo nhân vật"));

        return fight(character.getCharId(), enemyId);
    }

    @Transactional
    public BattleResult fight(Integer charId, Integer enemyId) {
        Character character = charRepo.findById(charId)
                .orElseThrow(() -> new RuntimeException("Character not found"));
        Enemy enemy = enemyRepo.findById(enemyId)
                .orElseThrow(() -> new RuntimeException("Enemy not found"));

        List<String> logs = new ArrayList<>();
        List<UserItem> equippedItems = userItemRepo.findByUser_UserIdAndIsEquippedTrue(character.getUser().getUserId());

        int totalAtk = character.getBaseAtk();
        int totalDef = character.getBaseDef();
        int totalCrit = character.getBaseCritRate();
        int extraHp = 0;

        for (UserItem ui : equippedItems) {
            totalAtk += ui.getItem().getAtkBonus();
            totalDef += ui.getItem().getDefBonus();
            totalCrit += ui.getItem().getCritRateBonus();
            extraHp += ui.getItem().getHpBonus();
        }

        int maxTotalHp = character.getMaxHp() + extraHp;
        int currentHp = maxTotalHp;
        int enemyHp = enemy.getHp();

        logs.add("⚔️ Bắt đầu: " + character.getName() + " vs " + enemy.getName());

        int turn = 0;
        boolean isWin = false;

        while (currentHp > 0 && enemyHp > 0 && turn < 50) {
            turn++;
            // Player đánh
            int dmgToEnemy = Math.max(1, totalAtk - enemy.getDef());
            if (random.nextInt(100) < totalCrit) {
                dmgToEnemy = (int) (dmgToEnemy * (character.getBaseCritDmg() / 100.0));
                logs.add("[Turn " + turn + "] 💥 BẠO KÍCH! Bạn gây " + dmgToEnemy + " sát thương!");
            } else {
                logs.add("[Turn " + turn + "] 🗡️ Bạn gây " + dmgToEnemy + " sát thương.");
            }
            enemyHp -= dmgToEnemy;

            if (enemyHp <= 0) {
                isWin = true;
                break;
            }

            // Quái đánh lại
            int dmgToChar = Math.max(1, enemy.getAtk() - totalDef);
            currentHp -= dmgToChar;
            logs.add("[Turn " + turn + "] 👾 " + enemy.getName() + " đánh lại " + dmgToChar + " sát thương!");
        }

        BattleResult result = new BattleResult();
        result.setEnemy(enemy);
        result.setPlayerHp(Math.max(0, currentHp));
        result.setPlayerMaxHp(maxTotalHp);
        result.setPlayerEnergy(character.getEnergy());
        result.setEnemyHp(Math.max(0, enemyHp));
        result.setEnemyMaxHp(enemy.getHp());
        result.setCombatLog(logs);
        result.setStatus(isWin ? "VICTORY" : "DEFEAT");

        if (isWin) {
            int exp = enemy.getExpReward();
            int gold = enemy.getGoldReward();

            character.setExp(character.getExp() + exp);
            long reqExp = character.getLv() * 100L;

            if (character.getExp() >= reqExp) {
                character.setExp((int)(character.getExp() - reqExp));
                character.setLv(character.getLv() + 1);
                character.setBaseAtk(character.getBaseAtk() + 5);
                character.setBaseDef(character.getBaseDef() + 2);
                character.setMaxHp(character.getMaxHp() + 50);
                character.setHp(character.getMaxHp());
                logs.add("🌟 CHÚC MỪNG! Bạn đã thăng cấp " + character.getLv());
                result.setLevelUp(true);
            } else {
                character.setHp(Math.min(currentHp, character.getMaxHp()));
            }

            Wallet wallet = walletRepo.findByUser_UserId(character.getUser().getUserId())
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));
            wallet.setGold(wallet.getGold().add(new BigDecimal(gold)));
            walletRepo.save(wallet);

            logs.add("🏆 Chiến thắng! Nhận được " + exp + " EXP và " + gold + " Vàng.");
            result.setExpEarned(exp);
            result.setGoldEarned(gold);

            // [NEW] GỌI HÀM DROP ITEM MỚI
            handleNewItemDrop(character.getUser(), logs, result, enemy.getLevel());

        } else {
            logs.add("💀 Bạn đã thất bại... Hãy luyện tập thêm.");
            character.setHp(1);
        }

        charRepo.save(character);
        return result;
    }

    // ====================================================================================
    // CƠ CHẾ DROP ĐỒ MỚI (STATS NGẪU NHIÊN + PHẨM CHẤT)
    // ====================================================================================
    private void handleNewItemDrop(User user, List<String> logs, BattleResult result, int enemyLv) {
        if (random.nextDouble() > DROP_RATE) return; // 30% tỷ lệ rơi

        // 1. Random Loại Trang Bị
        String type = pickRandomType();

        // 2. Random Phẩm Chất (Weight: S=5%, A=15%, B=30%, C=50%)
        String rarity = pickRarity();

        // 3. Tạo Item Mới (Unique - Không dùng chung template)
        Item item = new Item();
        item.setUser(user); // Gán chủ sở hữu ngay lập tức
        item.setType(type);
        item.setRarity(rarity);
        item.setIsSystemItem(false);
        item.setIsEquipped(false);
        // Giá bán = 100 + (Lv Quái * 10)
        item.setBasePrice(BigDecimal.valueOf(100 + (enemyLv * 10L)));

        // 4. Tạo Stats (Main + Subs) theo bảng cấu hình
        applyStats(item, type, rarity, enemyLv);

        // 5. Sinh Tên & Ảnh ngẫu nhiên
        item.setName(generateName(type, rarity));
        item.setImageUrl(generateImageCode(type)); // [FIX] Hàm này đã được sửa

        item.setDescription("Trang bị " + rarity + " rơi từ quái vật cấp " + enemyLv);

        // Lưu Item vào DB
        itemRepo.save(item);

        // Tạo UserItem liên kết
        UserItem ui = new UserItem();
        ui.setUser(user);
        ui.setItem(item);
        ui.setQuantity(1);
        ui.setIsEquipped(false);
        ui.setEnhanceLevel(0);
        userItemRepo.save(ui);

        logs.add("🎁 NHẶT ĐƯỢC: [" + item.getName() + "] - Phẩm chất: " + rarity);

        // Cập nhật thông tin trả về cho Frontend hiển thị popup
        result.setDroppedItemName(item.getName());
        result.setDroppedItemImage(item.getImageUrl());
        result.setDroppedItemRarity(rarity);
    }

    private String pickRandomType() {
        List<String> types = Arrays.asList("WEAPON", "ARMOR", "HELMET", "BOOTS", "RING", "NECKLACE");
        return types.get(random.nextInt(types.size()));
    }

    private String pickRarity() {
        int roll = random.nextInt(100);
        if (roll < 5) return "S";       // 5% Vàng (Legendary)
        if (roll < 20) return "A";      // 15% Tím (Epic)
        if (roll < 50) return "B";      // 30% Xanh dương (Rare)
        return "C";                     // 50% Xanh lá (Common)
    }

    private void applyStats(Item item, String type, String rarity, int lv) {
        // Reset stats mặc định
        item.setAtkBonus(0); item.setDefBonus(0); item.setHpBonus(0);
        item.setSpeedBonus(0); item.setCritRateBonus(0); item.setEnergyBonus(0);

        int scale = Math.max(1, lv * 2); // Hệ số stat tăng theo level quái để đồ mạnh dần

        // --- 1. MAIN STAT (Bảng 1) ---
        switch (type) {
            case "WEAPON": item.setAtkBonus(10 * scale); break; // 100% ATK
            case "HELMET": item.setHpBonus(50 * scale); break;  // 100% HP
            case "ARMOR":  item.setDefBonus(5 * scale); break;  // 100% DEF
            case "BOOTS":
                int roll = random.nextInt(100);
                if (roll < 5) item.setSpeedBonus(2 + (lv / 10)); // 5% Rare Speed
                else if (roll < 35) { // 30% Common Flat
                    int r = random.nextInt(3);
                    if(r==0) item.setAtkBonus(5 * scale);
                    else if(r==1) item.setHpBonus(20 * scale);
                    else item.setDefBonus(2 * scale);
                } else { // 65% Common % (Giả lập % bằng chỉ số flat cao hơn xíu)
                    int r = random.nextInt(3);
                    if(r==0) item.setAtkBonus(8 * scale);
                    else if(r==1) item.setHpBonus(35 * scale);
                    else item.setDefBonus(4 * scale);
                }
                break;
            case "RING":
            case "NECKLACE":
                // Random Main Stat cho trang sức
                int r = random.nextInt(3);
                if(r==0) item.setAtkBonus(5 * scale);
                else if(r==1) item.setHpBonus(30 * scale);
                else item.setDefBonus(3 * scale);

                // Necklace: 1/3 Tỉ lệ có dòng Crit DMG (Map tạm vào Crit Rate vì DB chưa có cột CritDmg)
                if (type.equals("NECKLACE") && random.nextInt(3) == 0) {
                    item.setCritRateBonus(2 + (lv / 20));
                }
                break;
        }

        // --- 2. SUB STATS (Bảng 2 & Số dòng theo Rarity) ---
        int subCount = 0;
        switch (rarity) {
            case "C": subCount = 1; break;
            case "B": subCount = 2; break;
            case "A": subCount = 3; break;
            case "S": subCount = 4; break;
        }

        for (int i = 0; i < subCount; i++) {
            addRandomSubStat(item, type, scale);
        }
    }

    private void addRandomSubStat(Item item, String type, int scale) {
        // Danh sách stat khả dụng
        List<String> allowedStats = new ArrayList<>(Arrays.asList("ATK", "DEF", "HP", "SPD", "CRIT"));

        // Blacklist Logic (Bảng 2 - Cấm các dòng vô lý)
        if (type.equals("WEAPON")) allowedStats.remove("DEF"); // Kiếm không thủ
        if (type.equals("HELMET")) allowedStats.remove("SPD"); // Mũ không tốc
        if (type.equals("ARMOR"))  allowedStats.remove("ATK"); // Áo không công

        String pick = allowedStats.get(random.nextInt(allowedStats.size()));

        // Cộng dồn vào chỉ số hiện có (Sub stat yếu hơn Main stat chút)
        switch (pick) {
            case "ATK": item.setAtkBonus(item.getAtkBonus() + (2 * scale)); break;
            case "DEF": item.setDefBonus(item.getDefBonus() + (1 * scale)); break;
            case "HP":  item.setHpBonus(item.getHpBonus() + (10 * scale)); break;
            case "SPD": item.setSpeedBonus(item.getSpeedBonus() + 1); break;
            case "CRIT": item.setCritRateBonus(item.getCritRateBonus() + 1); break;
        }
    }

    private String generateName(String type, String rarity) {
        String prefix = "";
        switch (rarity) {
            case "S": prefix = "Thần Thoại"; break;
            case "A": prefix = "Sử Thi"; break;
            case "B": prefix = "Tinh Anh"; break;
            case "C": prefix = "Thường"; break;
        }

        String baseName = "";
        switch (type) {
            case "WEAPON": baseName = "Kiếm"; break;
            case "ARMOR": baseName = "Giáp"; break;
            case "HELMET": baseName = "Mũ"; break;
            case "BOOTS": baseName = "Giày"; break;
            case "RING": baseName = "Nhẫn"; break;
            case "NECKLACE": baseName = "Dây Chuyền"; break;
        }

        return prefix + " " + baseName;
    }

    // [FIX] SỬA LẠI HÀM NÀY ĐỂ TRẢ VỀ TÊN FILE THAY VÌ FULL PATH
    private String generateImageCode(String type) {
        // Random từ 0 đến 4
        int variant = random.nextInt(5);

        switch (type) {
            case "WEAPON":   return "s_sword_" + variant + ".png";
            case "ARMOR":    return "a_armor_" + variant + ".png";
            case "HELMET":   return "h_helmet_" + variant + ".png";
            case "BOOTS":    return "b_boot_" + variant + ".png";
            case "RING":     return "ri_ring_" + variant + ".png";
            case "NECKLACE": return "n_necklace_" + variant + ".png";
            default:         return "s_sword_0.png";
        }
    }
}