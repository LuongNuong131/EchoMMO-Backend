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
        c.setLevel(1); // [FIX] setLv -> setLevel

        if (u.getRole() == Role.ADMIN) {
            // --- ADMIN SETUP ---
            c.setCurrentHp(9999); c.setMaxHp(9999); // [FIX] setHp -> setCurrentHp
            c.setCurrentEnergy(999); c.setMaxEnergy(999); // [FIX] setEnergy -> setCurrentEnergy

            c.setBaseAtk(999);
            c.setBaseDef(999);
            c.setBaseSpeed(999);
            c.setBaseCritRate(100);
            c.setBaseCritDmg(300);

            // Set stats khủng cho admin
            c.setStatPoints(9999);
            c.setStr(999);
            c.setVit(999);
            c.setAgi(999);
        } else {
            // --- USER THƯỜNG SETUP ---
            // 1. Khởi tạo chỉ số tiềm năng mặc định
            c.setStatPoints(5);
            c.setStr(5);
            c.setVit(5);
            c.setAgi(5);

            // 2. Tính toán chỉ số chiến đấu từ tiềm năng
            recalculateDerivedStats(c);

            // 3. Set các chỉ số còn lại
            c.setBaseCritRate(50); // 5%
            c.setCurrentEnergy(50); c.setMaxEnergy(50); // [FIX] setEnergy -> setCurrentEnergy
        }

        c.setStatus(CharacterStatus.IDLE);
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
    public Character addStats(int str, int vit, int agi) {
        Character character = getMyCharacter();
        if (character == null) {
            throw new RuntimeException("Không tìm thấy nhân vật");
        }

        int totalPointsNeeded = str + vit + agi;
        if (totalPointsNeeded <= 0) {
            throw new RuntimeException("Số điểm cộng phải lớn hơn 0");
        }

        if (character.getStatPoints() < totalPointsNeeded) {
            throw new RuntimeException("Không đủ điểm tiềm năng");
        }

        // 1. Trừ điểm tiềm năng
        character.setStatPoints(character.getStatPoints() - totalPointsNeeded);

        // 2. Cộng dồn vào chỉ số gốc (xử lý null safe)
        character.setStr((character.getStr() == null ? 5 : character.getStr()) + str);
        character.setVit((character.getVit() == null ? 5 : character.getVit()) + vit);
        character.setAgi((character.getAgi() == null ? 5 : character.getAgi()) + agi);

        // 3. Tính toán lại các chỉ số chiến đấu
        recalculateDerivedStats(character);

        return charRepo.save(character);
    }

    /**
     * Hàm tính toán lại chỉ số cơ bản dựa trên điểm tiềm năng (STR, VIT, AGI)
     */
    private void recalculateDerivedStats(Character c) {
        int baseHpConstant = 100;
        int baseAtkConstant = 10;
        int baseDefConstant = 5;
        int baseSpeedConstant = 10;
        int baseCritDmgConstant = 150;

        // Tính HP
        int newMaxHp = baseHpConstant + ((c.getVit() == null ? 0 : c.getVit()) * 10);
        c.setMaxHp(newMaxHp);
        c.setCurrentHp(newMaxHp); // [FIX] setHp -> setCurrentHp

        // Tính ATK
        c.setBaseAtk(baseAtkConstant + ((c.getStr() == null ? 0 : c.getStr()) * 2));

        // Tính DEF
        c.setBaseDef(baseDefConstant + ((c.getVit() == null ? 0 : c.getVit()) * 1));

        // Tính Speed (2 AGI = 1 Speed)
        c.setBaseSpeed(baseSpeedConstant + ((c.getAgi() == null ? 0 : c.getAgi()) / 2));

        // Tính Crit Dmg (2 STR = 1% Crit Dmg)
        c.setBaseCritDmg(baseCritDmgConstant + ((c.getStr() == null ? 0 : c.getStr()) / 2));
    }
}