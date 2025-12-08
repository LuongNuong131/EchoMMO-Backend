package com.echommo.service;

import com.echommo.dto.CharacterRequest;
import com.echommo.entity.Character;
import com.echommo.entity.*;
import com.echommo.repository.*;
import com.echommo.enums.CharacterStatus;
import com.echommo.enums.Role; // [ADD] Import Role
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
        c.setLv(1);

        // --- [FIX] CHECK ROLE ĐỂ SET CHỈ SỐ GỐC ---
        if (u.getRole() == Role.ADMIN) {
            // Admin: Full 999
            c.setHp(9999); c.setMaxHp(9999); // Máu trâu hơn chút
            c.setEnergy(999); c.setMaxEnergy(999);

            c.setBaseAtk(999);
            c.setBaseDef(999);
            c.setBaseSpeed(999);
            c.setBaseCritRate(100);  // Admin chắc 100% crit luôn cho oách? Hoặc để 999 tùy bạn
            c.setBaseCritDmg(300);   // 300% crit dmg
        } else {
            // User thường: Gốc = 0
            c.setHp(100); c.setMaxHp(100);
            c.setEnergy(50); c.setMaxEnergy(50);

            c.setBaseAtk(0);
            c.setBaseDef(0);
            c.setBaseSpeed(0);
            c.setBaseCritRate(1);
            c.setBaseCritDmg(150);
        }

        c.setStatus(CharacterStatus.IDLE);
        c = charRepo.save(c);

        // Tặng đồ tân thủ (Admin cũng tặng luôn cho vui)
        for(String n : Arrays.asList("Kiếm Gỗ", "Áo Vải", "Bình Máu")) {
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
}