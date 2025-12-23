package com.echommo.service;

import com.echommo.dto.CharacterRequest;
import com.echommo.dto.SubStatDTO;
import com.echommo.entity.Character;
import com.echommo.entity.Item;
import com.echommo.entity.User;
import com.echommo.entity.UserItem;
import com.echommo.enums.CharacterStatus;
import com.echommo.enums.Rarity;
import com.echommo.enums.SlotType;
import com.echommo.repository.CharacterRepository;
import com.echommo.repository.ItemRepository;
import com.echommo.repository.UserItemRepository;
import com.echommo.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class CharacterService {

    @Autowired private CharacterRepository characterRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private ItemRepository itemRepo;
    @Autowired private UserItemRepository userItemRepo;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // --- HELPER METHODS ---
    private BigDecimal safe(BigDecimal val) { return val == null ? BigDecimal.ZERO : val; }
    private int safeInt(Integer val) { return val == null ? 0 : val; }
    private BigDecimal safeIntToBig(Integer val) { return val == null ? BigDecimal.ZERO : BigDecimal.valueOf(val); }

    public Character getMyCharacter() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByUsername(username).orElse(null);
        if (user == null) return null;

        Character c = characterRepo.findByUser(user).orElse(null);
        if (c != null) {
            // [SAFEGUARD] Nếu tính toán lỗi, vẫn trả về nhân vật để không crash game
            recalculateStats(c);
        }
        return c;
    }

    @Transactional
    public Character createDefaultCharacter(User user) {
        return characterRepo.findByUser(user)
                .orElseGet(() -> initNewCharacter(user, user.getUsername()));
    }

    @Transactional
    public Character createCharacter(CharacterRequest req) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        if (characterRepo.findByUser(user).isPresent()) {
            throw new RuntimeException("Bạn đã có nhân vật rồi!");
        }
        return initNewCharacter(user, req.getName());
    }

    private Character initNewCharacter(User user, String charName) {
        Character c = new Character();
        c.setUser(user);
        c.setName(charName);
        c.setLevel(1);
        c.setCurrentExp(0L);
        c.setStatus(CharacterStatus.IDLE);
        c.setMonsterKills(0);

        // Stats khởi đầu
        c.setStr(5); c.setVit(5); c.setAgi(5);
        c.setDex(5); c.setIntelligence(5); c.setLuck(5);
        c.setStatPoints(0);

        c = characterRepo.save(c);
        grantStarterPack(c);
        recalculateStats(c);
        return characterRepo.save(c);
    }

    /**
     * [FIXED] Tự động tính lại cả Điểm Tiềm Năng khi gọi hàm này
     * [SAFEGUARD] Bọc try-catch để tránh crash API /me nếu dữ liệu lỗi
     */
    @Transactional
    public void recalculateStats(Character c) {
        try {
            ensureNoNullStats(c);

            // [FIX QUAN TRỌNG] Gọi hàm này để cập nhật điểm tiềm năng dựa trên Level hiện tại
            recalculateStatPoints(c);

            int lvl = safeInt(c.getLevel());

            // 1. CHỈ SỐ CƠ BẢN TỪ THUỘC TÍNH (STR, VIT...)
            int rawMaxHp = 100 + (safeInt(c.getVit()) * 15);
            int rawAtk = 5 + (safeInt(c.getStr()) * 2);
            int rawDef = 2 + (safeInt(c.getVit()) / 3);
            int rawSpeed = 10 + safeInt(c.getAgi());

            double totalCritRate = 1.0 + (safeInt(c.getLuck()) / 10.0);
            double totalCritDmg = 150.0 + (safeInt(c.getDex()) / 5.0);
            double totalSpeed = rawSpeed;

            // 2. CỘNG DỒN TỪ TRANG BỊ
            List<UserItem> equippedItems = userItemRepo.findByCharacter_CharIdAndIsEquippedTrue(c.getCharId());

            BigDecimal equipAtk = BigDecimal.ZERO;
            BigDecimal equipDef = BigDecimal.ZERO;
            BigDecimal equipHp = BigDecimal.ZERO;

            for (UserItem uItem : equippedItems) {
                // [Check] Độ bền (Handle Null Safety)
                Integer currentDur = uItem.getCurrentDurability();
                if (currentDur != null && currentDur <= 0) continue;

                // --- A. MAIN STAT (Dòng chính) ---
                BigDecimal val = safe(uItem.getMainStatValue());
                String type = uItem.getMainStatType();
                String typeUpper = (type != null) ? type.toUpperCase() : "";

                if (val.compareTo(BigDecimal.ZERO) > 0 && !typeUpper.isEmpty()) {
                    if (typeUpper.contains("ATK")) equipAtk = equipAtk.add(val);
                    else if (typeUpper.contains("DEF")) equipDef = equipDef.add(val);
                    else if (typeUpper.contains("HP")) equipHp = equipHp.add(val);
                    else if (typeUpper.contains("SPEED")) totalSpeed += val.doubleValue();
                    else if (typeUpper.contains("CRIT")) totalCritRate += val.doubleValue();

                    // Xử lý % nếu có
                    if (typeUpper.equals("ATK_PERCENT"))
                        equipAtk = equipAtk.add(BigDecimal.valueOf(rawAtk).multiply(val).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                    if (typeUpper.equals("HP_PERCENT"))
                        equipHp = equipHp.add(BigDecimal.valueOf(rawMaxHp).multiply(val).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                }

                // --- B. BASE STAT FALLBACK (Chỉ số gốc của Item) ---
                if (uItem.getItem() != null) {
                    Item tpl = uItem.getItem();
                    if (tpl.getAtkBonus() != null && !typeUpper.contains("ATK"))
                        equipAtk = equipAtk.add(BigDecimal.valueOf(tpl.getAtkBonus()));
                    if (tpl.getDefBonus() != null && !typeUpper.contains("DEF"))
                        equipDef = equipDef.add(BigDecimal.valueOf(tpl.getDefBonus()));
                    if (tpl.getHpBonus() != null && !typeUpper.contains("HP"))
                        equipHp = equipHp.add(BigDecimal.valueOf(tpl.getHpBonus()));
                    if (tpl.getSpeedBonus() != null && !typeUpper.contains("SPEED"))
                        totalSpeed += tpl.getSpeedBonus();
                }

                // --- C. SUBSTATS (Dòng ẩn) ---
                try {
                    if (uItem.getSubStats() != null && uItem.getSubStats().length() > 2) {
                        List<SubStatDTO> subs = objectMapper.readValue(uItem.getSubStats(), new TypeReference<List<SubStatDTO>>() {});
                        for (SubStatDTO sub : subs) {
                            double subVal = sub.getValue();
                            String subCode = (sub.getCode() != null) ? sub.getCode().toUpperCase() : "";

                            if (subCode.contains("ATK")) equipAtk = equipAtk.add(BigDecimal.valueOf(subVal));
                            else if (subCode.contains("DEF")) equipDef = equipDef.add(BigDecimal.valueOf(subVal));
                            else if (subCode.contains("HP")) equipHp = equipHp.add(BigDecimal.valueOf(subVal));
                            else if (subCode.contains("SPEED")) totalSpeed += subVal;
                            else if (subCode.contains("CRIT")) totalCritRate += subVal;
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            // 3. LƯU VÀO DATABASE
            c.setBaseAtk(rawAtk + equipAtk.setScale(0, RoundingMode.HALF_UP).intValue());
            c.setBaseDef(rawDef + equipDef.setScale(0, RoundingMode.HALF_UP).intValue());
            c.setMaxHp(rawMaxHp + equipHp.setScale(0, RoundingMode.HALF_UP).intValue());

            c.setBaseSpeed((int) Math.round(totalSpeed));
            c.setBaseCritRate((int) Math.round(totalCritRate));
            c.setBaseCritDmg((int) Math.round(totalCritDmg));

            // Tính lực chiến (Combat Power)
            int power = (c.getMaxHp() / 5) + (c.getBaseAtk() * 2) + (c.getBaseDef() * 3) + (c.getBaseSpeed() * 2) + (lvl * 10);
            c.setTotalPower(power);

            if (c.getCurrentHp() == null || c.getCurrentHp() > c.getMaxHp()) {
                c.setCurrentHp(c.getMaxHp());
            }

            characterRepo.saveAndFlush(c);
        } catch (Exception e) {
            // [IMPORTANT] Log lỗi nhưng KHÔNG throw exception để client vẫn nhận được data character
            System.err.println("⚠️ Lỗi recalculateStats (Có thể do DB lệch Schema hoặc Item rác): " + e.getMessage());
            // e.printStackTrace();
        }
    }

    @Transactional
    public Character addStats(Map<String, Integer> stats) {
        Character c = getMyCharacter();
        if (c == null) throw new RuntimeException("Chưa tạo nhân vật!");

        recalculateStatPoints(c);
        int available = safeInt(c.getStatPoints());

        int addStr = stats.getOrDefault("str", 0);
        int addVit = stats.getOrDefault("vit", 0);
        int addAgi = stats.getOrDefault("agi", 0);
        int addDex = stats.getOrDefault("dex", 0);
        int addInt = stats.getOrDefault("int", 0);
        int addLuck = stats.getOrDefault("luck", 0);

        int totalCost = addStr + addVit + addAgi + addDex + addInt + addLuck;

        if (totalCost <= 0) throw new IllegalArgumentException("Vui lòng chọn chỉ số!");
        if (totalCost > available) throw new IllegalArgumentException("Không đủ điểm!");

        c.setStr(safeInt(c.getStr()) + addStr);
        c.setVit(safeInt(c.getVit()) + addVit);
        c.setAgi(safeInt(c.getAgi()) + addAgi);
        c.setDex(safeInt(c.getDex()) + addDex);
        c.setIntelligence(safeInt(c.getIntelligence()) + addInt);
        c.setLuck(safeInt(c.getLuck()) + addLuck);

        c = characterRepo.save(c);
        recalculateStatPoints(c);
        recalculateStats(c);

        return c;
    }

    // Logic: Mỗi cấp được 5 điểm. Trừ đi số điểm đã cộng vào 6 chỉ số (tính từ mốc 5).
    private void recalculateStatPoints(Character c) {
        int lvl = safeInt(c.getLevel());
        int totalPointsEarned = (lvl - 1) * 5; // Cấp 1 có 0 điểm, Cấp 2 có 5 điểm...

        int usedPoints = (safeInt(c.getStr()) - 5) +
                (safeInt(c.getVit()) - 5) +
                (safeInt(c.getAgi()) - 5) +
                (safeInt(c.getDex()) - 5) +
                (safeInt(c.getIntelligence()) - 5) +
                (safeInt(c.getLuck()) - 5);

        c.setStatPoints(Math.max(0, totalPointsEarned - usedPoints));
    }

    private void ensureNoNullStats(Character c) {
        if (c.getStr() == null) c.setStr(5);
        if (c.getVit() == null) c.setVit(5);
        if (c.getAgi() == null) c.setAgi(5);
        if (c.getDex() == null) c.setDex(5);
        if (c.getIntelligence() == null) c.setIntelligence(5);
        if (c.getLuck() == null) c.setLuck(5);
        if (c.getLevel() == null) c.setLevel(1);
        if (c.getCurrentExp() == null) c.setCurrentExp(0L);
    }

    private void grantStarterPack(Character character) {
        itemRepo.findByName("Kiếm Gỗ").ifPresent(i -> createItemForChar(character, i, 1, true));
        itemRepo.findByName("Áo Vải Rách").ifPresent(i -> createItemForChar(character, i, 1, true));
    }

    private void createItemForChar(Character c, Item item, int qty, boolean equip) {
        UserItem ui = new UserItem();
        ui.setCharacter(c);
        ui.setItem(item);
        ui.setQuantity(qty);
        ui.setIsEquipped(equip);
        ui.setRarity(Rarity.COMMON);
        ui.setEnhanceLevel(0);

        if (item.getSlotType() == SlotType.WEAPON) {
            ui.setMainStatType("ATK_FLAT");
            BigDecimal val = safeIntToBig(item.getAtkBonus());
            ui.setMainStatValue(val.compareTo(BigDecimal.ZERO) > 0 ? val : BigDecimal.valueOf(5));
        } else if (item.getSlotType() == SlotType.ARMOR) {
            ui.setMainStatType("DEF_FLAT");
            BigDecimal val = safeIntToBig(item.getDefBonus());
            ui.setMainStatValue(val.compareTo(BigDecimal.ZERO) > 0 ? val : BigDecimal.valueOf(2));
        } else {
            ui.setMainStatType("HP_FLAT");
            ui.setMainStatValue(BigDecimal.valueOf(10));
        }

        ui.setOriginalMainStatValue(ui.getMainStatValue());
        ui.setSubStats("[]");
        ui.setAcquiredAt(LocalDateTime.now());
        ui.setCurrentDurability(item.getMaxDurability() != null ? item.getMaxDurability() : 100);
        ui.setMaxDurability(item.getMaxDurability() != null ? item.getMaxDurability() : 100);

        userItemRepo.save(ui);
    }
}