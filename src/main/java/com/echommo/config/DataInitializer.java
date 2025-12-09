package com.echommo.config;

import com.echommo.entity.*;
import com.echommo.entity.Character;
import com.echommo.enums.CharacterStatus;
import com.echommo.enums.Rarity;
import com.echommo.enums.Role;
import com.echommo.enums.SlotType;
import com.echommo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private UserRepository userRepo;
    @Autowired private CharacterRepository charRepo;
    @Autowired private ItemRepository itemRepo;
    @Autowired private UserItemRepository userItemRepo;
    @Autowired private WalletRepository walletRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 1. Tạo Items mặc định nếu database trống
        if (itemRepo.count() == 0) {
            System.out.println(">>> Đang khởi tạo vật phẩm mẫu...");
            createDefaultItems();
        }

        // 2. Tạo Admin User nếu chưa có
        if (!userRepo.existsByUsername("admin")) {
            System.out.println(">>> Đang tạo tài khoản Admin...");
            createAdminAccount();
        }
    }

    private void createDefaultItems() {
        // --- Nguyên liệu (Dùng cho Explore & Cường hóa) ---
        createItem("Gỗ", "MATERIAL", Rarity.COMMON, 1, "r_go.png", SlotType.NONE);
        createItem("Đá", "MATERIAL", Rarity.COMMON, 1, "stone_1.png", SlotType.NONE);
        createItem("Quặng Đồng", "MATERIAL", Rarity.COMMON, 1, "r_copper_node.png", SlotType.NONE);
        createItem("Sắt", "MATERIAL", Rarity.RARE, 2, "r_silver_node.png", SlotType.NONE);
        createItem("Cá", "MATERIAL", Rarity.COMMON, 1, "r_sliver_coin.png", SlotType.NONE);
        createItem("Bạch Kim", "MATERIAL", Rarity.EPIC, 3, "r_mystrile_node.png", SlotType.NONE);

        // --- Trang bị Tân thủ ---
        createItem("Kiếm Gỗ", "WEAPON", Rarity.COMMON, 1, "s_sword_0.png", SlotType.WEAPON);
        createItem("Áo Vải", "ARMOR", Rarity.COMMON, 1, "a_armor_0.png", SlotType.ARMOR);
        createItem("Bình Máu Nhỏ", "CONSUMABLE", Rarity.COMMON, 1, "r_sliver_coin.png", SlotType.NONE);
    }

    private void createItem(String name, String type, Rarity rarity, int tier, String img, SlotType slot) {
        Item item = new Item();
        item.setName(name);
        item.setType(type);
        item.setRarity(rarity);
        item.setTier(tier);
        item.setImageUrl(img);
        item.setSlotType(slot);
        item.setDescription("Vật phẩm hệ thống tạo.");
        itemRepo.save(item);
    }

    private void createAdminAccount() {
        User admin = new User();
        admin.setUsername("admin");
//        admin.setPassword(passwordEncoder.encode("admin123")); // Mật khẩu là admin123
        admin.setRole(Role.ADMIN);
        admin.setEmail("admin@echommo.com");
        userRepo.save(admin);

        // Tạo ví tiền
        Wallet w = new Wallet();
        w.setUser(admin);
        w.setGold(new BigDecimal("999999"));
        walletRepo.save(w);

        // Tạo nhân vật Admin bá đạo
        Character c = new Character();
        c.setUser(admin);
        c.setName("GM_Admin");
        c.setLevel(99);
        c.setStatus(CharacterStatus.IDLE);

        // [FIX] Sửa lỗi setVit/setAgi thành setIntelligence/setDex
        c.setStr(999);
        c.setDex(999);            // Thay cho Agi
        c.setIntelligence(999);   // Thay cho Vit
        c.setLuck(999);           // Mới thêm

        // Chỉ số chiến đấu
        c.setBaseAtk(9999);
        c.setBaseDef(9999);
        c.setBaseSpeed(999);
        c.setCurrentHp(99999); c.setMaxHp(99999);
        c.setCurrentEnergy(9999); c.setMaxEnergy(9999);

        c.setStatPoints(9999);

        charRepo.save(c);
        System.out.println(">>> Tạo Admin thành công! (User: admin / Pass: admin123)");
    }
}