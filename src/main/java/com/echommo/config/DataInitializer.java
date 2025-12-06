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

        // 3. Tạo Nhân Vật (Fix lỗi 400)
        if (charRepo.findByUser_UserId(admin.getUserId()).isEmpty()) {
            Character c = new Character();
            c.setUser(admin);
            c.setName("Yasuo"); // Tên ngầu tí
            c.setLv(99);
            c.setExp(0);
            c.setHp(9999); c.setMaxHp(9999);
            c.setEnergy(9999); c.setMaxEnergy(9999);
            c.setBaseAtk(500); c.setBaseDef(200); c.setBaseSpeed(50);
            c.setBaseCritRate(50); c.setBaseCritDmg(200);
            c.setStatus(CharacterStatus.IDLE);
            charRepo.save(c);
            System.out.println(">> [INIT] Đã tạo nhân vật Yasuo cho Admin!");

            // 4. Tặng đồ
            giveItemsToUser(admin);
        }
    }

    private void giveItemsToUser(User user) {
        List<String> starterItems = Arrays.asList("Kiếm Sắt", "Áo Vải", "Bình Máu");
        for (String itemName : starterItems) {
            Optional<Item> itemOpt = itemRepo.findByName(itemName);
            if (itemOpt.isPresent()) {
                UserItem ui = new UserItem();
                ui.setUser(user);
                ui.setItem(itemOpt.get());
                ui.setQuantity(10);
                ui.setIsEquipped(false);
                ui.setEnhanceLevel(0);
                userItemRepo.save(ui);
            }
        }
    }
}