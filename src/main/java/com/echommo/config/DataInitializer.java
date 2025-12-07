package com.echommo.config;

import com.echommo.entity.*;
import com.echommo.entity.Character;
import com.echommo.repository.*;
import com.echommo.enums.CharacterStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private UserRepository userRepo;
    @Autowired private CharacterRepository charRepo;
    @Autowired private WalletRepository walletRepo;
    @Autowired private ItemRepository itemRepo;
    @Autowired private UserItemRepository userItemRepo;
    @Autowired private PasswordEncoder encoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Tạo items mẫu (6 Món cho full set)
        createSystemItems();

        // 1. Tạo Admin User
        User admin = userRepo.findByUsername("admin").orElseGet(() -> {
            User u = new User();
            u.setUsername("admin");
            u.setEmail("admin@echommo.com");
            u.setPasswordHash(encoder.encode("123456"));
            u.setRole(com.echommo.enums.Role.ADMIN);
            u.setFullName("Game Master");
            return userRepo.save(u);
        });

        // 2. Tạo Ví
        if (walletRepo.findByUser_UserId(admin.getUserId()).isEmpty()) {
            Wallet w = new Wallet();
            w.setUser(admin);
            w.setGold(new BigDecimal("99999"));
            w.setDiamonds(9999);
            walletRepo.save(w);
        }

        // 3. Tạo Nhân Vật
        if (charRepo.findByUser_UserId(admin.getUserId()).isEmpty()) {
            Character c = new Character();
            c.setUser(admin);
            c.setName("Yasuo");
            c.setLv(99);
            c.setExp(0);
            c.setHp(9999); c.setMaxHp(9999);
            c.setEnergy(9999); c.setMaxEnergy(9999);
            c.setBaseAtk(500); c.setBaseDef(200); c.setBaseSpeed(50);
            c.setBaseCritRate(50); c.setBaseCritDmg(200);
            c.setStatus(CharacterStatus.IDLE);
            charRepo.save(c);
            System.out.println(">> [INIT] Đã tạo nhân vật Yasuo cho Admin!");

            // 4. Tặng Full Set Đồ
            giveFullSetToUser(admin);
        }
    }

    private void createSystemItems() {
        // Weapon
        createItemIfMissing("Hỏa Long Kiếm", "Thanh kiếm rực lửa.", "WEAPON", "S", 1000, "s_sword_1", 100, 0, 0);
        // Armor
        createItemIfMissing("Hắc Ám Giáp", "Giáp từ vực thẳm.", "ARMOR", "S", 1000, "a_armor_1", 0, 50, 200);
        // Helmet
        createItemIfMissing("Chiến Thần Mão", "Mũ của chiến thần.", "HELMET", "S", 800, "h_helmet_1", 0, 30, 100);
        // Boots
        createItemIfMissing("Hỏa Vân Hài", "Đi mây về gió.", "BOOTS", "S", 800, "b_boot_1", 0, 10, 50); // Speed logic sau
        // Ring
        createItemIfMissing("Nhẫn Rồng", "Tăng sức mạnh.", "RING", "A", 1500, "ri_ring_1", 50, 0, 0);
        // Necklace
        createItemIfMissing("Vòng Cổ Ngọc", "Tăng sinh lực.", "NECKLACE", "A", 1500, "n_neck_1", 0, 0, 300);
    }

    private void createItemIfMissing(String name, String desc, String type, String rarity, int price, String imgCode, int atk, int def, int hp) {
        if (itemRepo.findByName(name).isEmpty()) {
            Item i = new Item();
            i.setName(name);
            i.setDescription(desc);
            i.setType(type);
            i.setRarity(rarity);
            i.setBasePrice(new BigDecimal(price));
            i.setImageUrl(imgCode); // Lưu mã code để frontend map
            i.setIsSystemItem(true);
            i.setAtkBonus(atk);
            i.setDefBonus(def);
            i.setHpBonus(hp);
            itemRepo.save(i);
        }
    }

    private void giveFullSetToUser(User user) {
        List<String> fullSet = Arrays.asList(
                "Hỏa Long Kiếm", "Hắc Ám Giáp", "Chiến Thần Mão",
                "Hỏa Vân Hài", "Nhẫn Rồng", "Vòng Cổ Ngọc"
        );

        for (String itemName : fullSet) {
            Optional<Item> itemOpt = itemRepo.findByName(itemName);
            if (itemOpt.isPresent()) {
                UserItem ui = new UserItem();
                ui.setUser(user);
                ui.setItem(itemOpt.get());
                ui.setQuantity(1);
                ui.setIsEquipped(true); // Mặc luôn cho ngầu
                ui.setEnhanceLevel(10);
                userItemRepo.save(ui);
            }
        }
    }
}