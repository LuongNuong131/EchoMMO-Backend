package com.echommo.service;

import com.echommo.dto.CharacterRequest;
import com.echommo.entity.Character;
import com.echommo.entity.*;
import com.echommo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.Optional;

@Service
public class CharacterService {
    @Autowired private CharacterRepository charRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private ItemRepository itemRepo;
    @Autowired private UserItemRepository uiRepo;

    public Character getMyCharacter() {
        String u = SecurityContextHolder.getContext().getAuthentication().getName();
        return charRepo.findByUser_UserId(userRepo.findByUsername(u).get().getUserId()).orElse(null);
    }

    @Transactional
    public Character createCharacter(CharacterRequest req) {
        User u = userRepo.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName()).get();
        if(charRepo.findByUser_UserId(u.getUserId()).isPresent()) throw new RuntimeException("Exists");
        if(charRepo.existsByName(req.getName())) throw new RuntimeException("Taken");

        Character c = new Character();
        c.setUser(u);
        c.setName(req.getName());

        // FIX: Sửa lỗi Setter - Dùng setLv, setBaseAtk, setBaseDef, v.v.
        c.setLv(1);
        c.setHp(100); c.setMaxHp(100);
        c.setEnergy(50); c.setMaxEnergy(50);
        c.setBaseAtk(10); c.setBaseDef(5);
        c.setBaseSpeed(10);
        c.setBaseCritRate(5);
        c.setBaseCritDmg(150);

        c = charRepo.save(c);

        for(String n : Arrays.asList("Kiếm Gỗ Tập Luyện", "Áo Vải Thô", "Bình Máu Nhỏ")) {
            Optional<Item> i = itemRepo.findByName(n);
            if(i.isPresent()) {
                UserItem ui = new UserItem();
                ui.setUser(u); ui.setItem(i.get()); ui.setQuantity(n.contains("Máu")?5:1); ui.setIsEquipped(false); ui.setEnhanceLevel(0);
                uiRepo.save(ui);
            }
        }
        return c;
    }

    @Transactional
    public String renameCharacter(String name) {
        if(charRepo.existsByName(name)) throw new RuntimeException("Tên đã tồn tại");
        Character c = getMyCharacter();
        c.setName(name);
        charRepo.save(c);
        return "Đổi tên thành công: "+name;
    }
}