//package com.echommo.service;
//
//import com.echommo.dto.CharacterRequest;
//import com.echommo.entity.Character;
//import com.echommo.entity.*;
//import com.echommo.repository.*;
//import com.echommo.enums.CharacterStatus;
//import com.echommo.enums.Role;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import java.util.Arrays;
//import java.util.Optional;
//import java.util.Random;
//
//@Service
//public class CharacterService {
//    @Autowired private CharacterRepository charRepo;
//    @Autowired private UserRepository userRepo;
//    @Autowired private ItemRepository itemRepo;
//    @Autowired private UserItemRepository uiRepo;
//
//    public Character getMyCharacter() {
//        try {
//            String u = SecurityContextHolder.getContext().getAuthentication().getName();
//            Optional<User> userOpt = userRepo.findByUsername(u);
//            if (userOpt.isEmpty()) return null;
//            return charRepo.findByUser_UserId(userOpt.get().getUserId()).orElse(null);
//        } catch (Exception e) {
//            return null;
//        }
//    }
//
//    @Transactional
//    public Character createDefaultCharacter(User user) {
//        Optional<Character> existing = charRepo.findByUser_UserId(user.getUserId());
//        if (existing.isPresent()) return existing.get();
//
//        CharacterRequest req = new CharacterRequest();
//        String baseName = user.getUsername();
//        String finalName = baseName;
//        int retries = 0;
//        while (charRepo.existsByName(finalName) && retries < 5) {
//            finalName = baseName + "_" + new Random().nextInt(1000);
//            retries++;
//        }
//        req.setName(finalName);
//        return createCharacterInternal(user, req);
//    }
//
//    @Transactional
//    public Character createCharacter(CharacterRequest req) {
//        User u = userRepo.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName()).get();
//        if(charRepo.findByUser_UserId(u.getUserId()).isPresent()) throw new RuntimeException("Character exists");
//        if(charRepo.existsByName(req.getName())) throw new RuntimeException("Name taken");
//        return createCharacterInternal(u, req);
//    }
//
//    private Character createCharacterInternal(User u, CharacterRequest req) {
//        Character c = new Character();
//        c.setUser(u);
//        c.setName(req.getName());
//        c.setLevel(1);
//        c.setStatus(CharacterStatus.IDLE);
//
//        if (u.getRole() == Role.ADMIN) {
//            // --- ADMIN SETUP ---
//            c.setCurrentHp(9999); c.setMaxHp(9999);
//            c.setCurrentEnergy(999); c.setMaxEnergy(999);
//
//            c.setBaseAtk(999);
//            c.setBaseDef(999);
//            c.setBaseSpeed(999);
//            c.setBaseCritRate(100);
//            c.setBaseCritDmg(300);
//
//            // Set stats khủng cho admin
//            c.setStatPoints(9999);
//            c.setStr(999);
//            c.setDex(999);          // [FIX] Agi -> Dex
//            c.setIntelligence(999); // [FIX] Vit -> Intelligence
//            c.setLuck(999);         // [FIX] Thêm Luck
//        } else {
//            // --- USER THƯỜNG SETUP ---
//            // 1. Khởi tạo chỉ số tiềm năng mặc định (Mỗi loại 5 điểm)
//            c.setStatPoints(5);
//            c.setStr(5);
//            c.setDex(5);           // [FIX] Agi -> Dex
//            c.setIntelligence(5);  // [FIX] Vit -> Intelligence
//            c.setLuck(5);          // [FIX] Thêm Luck
//
//            // 2. Tính toán chỉ số chiến đấu từ tiềm năng
//            recalculateDerivedStats(c);
//
//            // 3. Set các chỉ số còn lại
//            c.setBaseCritRate(50); // 5%
//            c.setCurrentEnergy(50);
//            c.setMaxEnergy(50);
//        }
//
//        c = charRepo.save(c);
//
//        // Tặng đồ tân thủ
//        for(String n : Arrays.asList("Kiếm Gỗ", "Áo Vải", "Bình Máu Nhỏ")) {
//            Optional<Item> i = itemRepo.findByName(n);
//            if(i.isPresent()) {
//                UserItem ui = new UserItem();
//                ui.setUser(u); ui.setItem(i.get()); ui.setQuantity(1); ui.setIsEquipped(false); ui.setEnhanceLevel(0);
//                uiRepo.save(ui);
//            }
//        }
//        return c;
//    }
//
//    @Transactional
//    public String renameCharacter(String name) {
//        if(charRepo.existsByName(name)) throw new RuntimeException("Tên đã tồn tại");
//        Character c = getMyCharacter();
//        if (c == null) throw new RuntimeException("Chưa có nhân vật");
//        c.setName(name);
//        charRepo.save(c);
//        return "Đổi tên thành công: "+name;
//    }
//
//    // =========================================================
//    // 👇 LOGIC CỘNG ĐIỂM TIỀM NĂNG & TÍNH STATS
//    // =========================================================
//
//    // [FIX] Đổi tham số thành: str, dex, intelligence, luck
//    @Transactional
//    public Character addStats(int str, int dex, int intelligence, int luck) {
//        Character character = getMyCharacter();
//        if (character == null) {
//            throw new RuntimeException("Không tìm thấy nhân vật");
//        }
//
//        int totalPointsNeeded = str + dex + intelligence + luck;
//        if (totalPointsNeeded <= 0) {
//            throw new RuntimeException("Số điểm cộng phải lớn hơn 0");
//        }
//
//        if (character.getStatPoints() < totalPointsNeeded) {
//            throw new RuntimeException("Không đủ điểm tiềm năng");
//        }
//
//        // 1. Trừ điểm tiềm năng
//        character.setStatPoints(character.getStatPoints() - totalPointsNeeded);
//
//        // 2. Cộng dồn vào chỉ số gốc (xử lý null safe)
//        character.setStr((character.getStr() == null ? 5 : character.getStr()) + str);
//        character.setDex((character.getDex() == null ? 5 : character.getDex()) + dex);
//        character.setIntelligence((character.getIntelligence() == null ? 5 : character.getIntelligence()) + intelligence);
//        character.setLuck((character.getLuck() == null ? 5 : character.getLuck()) + luck);
//
//        // 3. Tính toán lại các chỉ số chiến đấu
//        recalculateDerivedStats(character);
//
//        return charRepo.save(character);
//    }
//
//    /**
//     * Hàm tính toán lại chỉ số cơ bản dựa trên điểm tiềm năng (STR, DEX, INT, LUCK)
//     */
//    private void recalculateDerivedStats(Character c) {
//        int baseHpConstant = 100;
//        int baseAtkConstant = 10;
//        int baseDefConstant = 5;
//        int baseSpeedConstant = 10;
//
//        // --- QUY ƯỚC TÍNH STATS MỚI ---
//
//        // 1. STR (Sức mạnh) -> Tăng ATK và HP nhẹ
//        int str = c.getStr() == null ? 0 : c.getStr();
//        c.setBaseAtk(baseAtkConstant + (str * 2));
//
//        // 2. DEX (Khéo léo) -> Tăng Tốc độ và Crit Rate
//        int dex = c.getDex() == null ? 0 : c.getDex();
//        c.setBaseSpeed(baseSpeedConstant + (dex / 2));
//        // Base Crit = 50 (5%) + Dex (ví dụ 1 Dex = 0.1% Crit)
//        c.setBaseCritRate(50 + (dex * 2));
//
//        // 3. INT (Trí tuệ) -> Tăng Max HP và Def (Thay cho VIT cũ)
//        int intel = c.getIntelligence() == null ? 0 : c.getIntelligence();
//        int newMaxHp = baseHpConstant + (intel * 15) + (str * 5); // HP chủ yếu từ Int (cơ thể cường tráng do tu luyện nội công :v)
//        c.setMaxHp(newMaxHp);
//        c.setCurrentHp(newMaxHp); // Hồi full máu khi update stats
//
//        c.setBaseDef(baseDefConstant + (intel * 1));
//
//        // 4. LUCK (May mắn) -> Tăng Crit Damage
//        int luck = c.getLuck() == null ? 0 : c.getLuck();
//        c.setBaseCritDmg(150 + (luck * 1)); // 1 Luck = +1% Crit Damage
//    }
//}
//
//


package com.echommo.service;

import com.echommo.dto.CharacterRequest;
import com.echommo.entity.Character;
import com.echommo.entity.*;
import com.echommo.repository.*;
import com.echommo.enums.CharacterStatus;
import com.echommo.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

@Service
public class CharacterService {
    @Autowired private CharacterRepository charRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private ItemRepository itemRepo;
    @Autowired private UserItemRepository uiRepo;

    public Character getMyCharacter() {
        try {
            String u = SecurityContextHolder.getContext().getAuthentication().getName();
            Optional<User> userOpt = userRepo.findByUsername(u);
            if (userOpt.isEmpty()) return null;
            return charRepo.findByUser_UserId(userOpt.get().getUserId()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
    

    @Transactional
    public Character createDefaultCharacter(User user) {
        Optional<Character> existing = charRepo.findByUser_UserId(user.getUserId());
        if (existing.isPresent()) return existing.get();

        CharacterRequest req = new CharacterRequest();
        String baseName = user.getUsername();
        String finalName = baseName;
        int retries = 0;
        while (charRepo.existsByName(finalName) && retries < 5) {
            finalName = baseName + "_" + new Random().nextInt(1000);
            retries++;
        }
        req.setName(finalName);
        return createCharacterInternal(user, req);
    }

    @Transactional
    public Character createCharacter(CharacterRequest req) {
        User u = userRepo.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName()).get();
        if(charRepo.findByUser_UserId(u.getUserId()).isPresent()) throw new RuntimeException("Character exists");
        if(charRepo.existsByName(req.getName())) throw new RuntimeException("Name taken");
        return createCharacterInternal(u, req);
    }

    private Character createCharacterInternal(User u, CharacterRequest req) {
        Character c = new Character();
        c.setUser(u);
        c.setName(req.getName());
        c.setLevel(1);
        c.setStatus(CharacterStatus.IDLE);

        if (u.getRole() == Role.ADMIN) {
            // --- ADMIN SETUP (Full Chỉ số) ---
            c.setCurrentHp(9999); c.setMaxHp(9999);
            c.setCurrentEnergy(999); c.setMaxEnergy(999);

            c.setBaseAtk(999);
            c.setBaseDef(999);
            c.setBaseSpeed(999);
            c.setBaseCritRate(100);
            c.setBaseCritDmg(300);

            // Set stats khủng
            c.setStatPoints(9999);
            c.setStr(999);
            c.setVit(999);
            c.setAgi(999);
            c.setDex(999);
            c.setIntelligence(999);
            c.setLuck(999);
        } else {
            // --- USER THƯỜNG SETUP (Mặc định 5 điểm) ---
            c.setStatPoints(5);
            c.setStr(5);
            c.setVit(5);
            c.setAgi(5);
            c.setDex(5);
            c.setIntelligence(5);
            c.setLuck(5);

            // Tính toán chỉ số chiến đấu từ stats
            recalculateDerivedStats(c);

            // Set năng lượng khởi đầu
            c.setCurrentEnergy(50);
            c.setMaxEnergy(50);
        }

        c = charRepo.save(c);

        // Tặng đồ tân thủ
        for(String n : Arrays.asList("Kiếm Gỗ", "Áo Vải", "Bình Máu Nhỏ")) {
            Optional<Item> i = itemRepo.findByName(n);
            if(i.isPresent()) {
                UserItem ui = new UserItem();
                ui.setUser(u); ui.setItem(i.get()); ui.setQuantity(1); ui.setIsEquipped(false); ui.setEnhanceLevel(0);
                uiRepo.save(ui);
            }
        }
        return c;
    }

    @Transactional
    public String renameCharacter(String name) {
        if(charRepo.existsByName(name)) throw new RuntimeException("Tên đã tồn tại");
        Character c = getMyCharacter();
        if (c == null) throw new RuntimeException("Chưa có nhân vật");
        c.setName(name);
        charRepo.save(c);
        return "Đổi tên thành công: "+name;
    }

    // =========================================================
    // 👇 LOGIC CỘNG ĐIỂM TIỀM NĂNG & TÍNH STATS
    // =========================================================

    @Transactional
    public Character addStats(int str, int vit, int agi, int dex, int intelligence, int luck) {
        Character character = getMyCharacter();
        if (character == null) {
            throw new RuntimeException("Không tìm thấy nhân vật");
        }

        int totalPointsNeeded = str + vit + agi + dex + intelligence + luck;
        if (totalPointsNeeded <= 0) {
            throw new RuntimeException("Số điểm cộng phải lớn hơn 0");
        }

        if (character.getStatPoints() < totalPointsNeeded) {
            throw new RuntimeException("Không đủ điểm tiềm năng");
        }

        // 1. Trừ điểm tiềm năng
        character.setStatPoints(character.getStatPoints() - totalPointsNeeded);

        // 2. Cộng dồn vào chỉ số gốc (xử lý null safe)
        character.setStr(getSafeVal(character.getStr()) + str);
        character.setVit(getSafeVal(character.getVit()) + vit);
        character.setAgi(getSafeVal(character.getAgi()) + agi);
        character.setDex(getSafeVal(character.getDex()) + dex);
        character.setIntelligence(getSafeVal(character.getIntelligence()) + intelligence);
        character.setLuck(getSafeVal(character.getLuck()) + luck);

        // 3. Tính toán lại các chỉ số chiến đấu
        recalculateDerivedStats(character);

        return charRepo.save(character);
    }

    // Helper xử lý null để tránh lỗi NullPointerException
    private int getSafeVal(Integer val) {
        return val == null ? 0 : val;
    }

    /**
     * Hàm tính toán lại chỉ số cơ bản dựa trên 6 chỉ số tiềm năng
     */
    private void recalculateDerivedStats(Character c) {
        int baseHpConstant = 100;
        int baseAtkConstant = 10;
        int baseDefConstant = 5;
        int baseSpeedConstant = 10;

        // Lấy giá trị an toàn từ Entity
        int str = getSafeVal(c.getStr());
        int vit = getSafeVal(c.getVit());
        int agi = getSafeVal(c.getAgi());
        int dex = getSafeVal(c.getDex());
        int intel = getSafeVal(c.getIntelligence());
        int luck = getSafeVal(c.getLuck());

        // --- CÔNG THỨC TÍNH STATS ---

        // 1. HP (Máu) = Base + (VIT * 20)
        int newMaxHp = baseHpConstant + (vit * 20);
        c.setMaxHp(newMaxHp);
        c.setCurrentHp(newMaxHp); // Hồi full máu khi cộng điểm

        // 2. ATK (Tấn công) = Base + (STR * 2) + (INT * 1)
        c.setBaseAtk(baseAtkConstant + (str * 2) + intel);

        // 3. DEF (Phòng thủ) = Base + (VIT * 1) + (STR * 0.5)
        c.setBaseDef(baseDefConstant + vit + (str / 2));

        // 4. SPEED (Tốc độ) = Base + (AGI * 1)
        c.setBaseSpeed(baseSpeedConstant + agi);

        // 5. CRIT RATE (Tỷ lệ bạo kích) = 50 (5%) + (DEX * 2)
        // Ví dụ: 10 DEX -> 50 + 20 = 70 (tức 7%)
        c.setBaseCritRate(50 + (dex * 2));

        // 6. CRIT DMG (Sát thương bạo kích) = 150% + (LUCK * 1%)
        c.setBaseCritDmg(150 + luck);
    }
}