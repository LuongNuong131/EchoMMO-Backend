package com.echommo.config;

import com.echommo.entity.*;
import com.echommo.entity.Character;
import com.echommo.enums.Rarity; // [NEW] Import Enum
import com.echommo.enums.SlotType; // [NEW] Import Enum
import com.echommo.repository.*;
import com.echommo.enums.CharacterStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        // 1. Tạo items mẫu (Item Template)
        createSystemItems();

        // 2. Tạo Admin User
        User admin = userRepo.findByUsername("admin").orElseGet(() -> {
            User u = new User();
            u.setUsername("admin");
            u.setEmail("admin@echommo.com");
            u.setPasswordHash(encoder.encode("123456"));
            u.setRole(com.echommo.enums.Role.ADMIN);
            u.setFullName("Game Master");
            u.setIsActive(true);
            return userRepo.save(u);
        });

        // 3. Tạo Ví
        if (walletRepo.findByUser_UserId(admin.getUserId()).isEmpty()) {
            Wallet w = new Wallet();
            w.setUser(admin);
            w.setGold(new BigDecimal("99999"));
            w.setDiamonds(9999);
            walletRepo.save(w);
        }

        // 4. Tạo Nhân Vật
        if (charRepo.findByUser_UserId(admin.getUserId()).isEmpty()) {
            Character c = new Character();
            c.setUser(admin);
            c.setName("Yasuo");
            c.setLevel(99); // [FIX] Đã đổi tên biến thành level
            c.setCurrentExp(0); // [FIX] Đã đổi tên biến

            // Set Stats Admin
            c.setMaxHp(99999); c.setCurrentHp(99999);
            c.setMaxEnergy(9999); c.setCurrentEnergy(9999);

            c.setBaseAtk(5000);
            c.setBaseDef(2000);
            c.setBaseSpeed(50);
            c.setBaseCritRate(1000); // 100%
            c.setBaseCritDmg(300); // 300%

            // Set điểm tiềm năng
            c.setStatPoints(9999);
            c.setStr(999); c.setVit(999); c.setAgi(999);

            c.setStatus(CharacterStatus.IDLE);
            charRepo.save(c);
            System.out.println(">> [INIT] Đã tạo nhân vật Yasuo cho Admin!");

            // 5. Tặng Full Set Đồ (Đã fix stats JSON)
            giveFullSetToUser(admin);
        }
    }

    private void createSystemItems() {
        // Weapon
        createItemIfMissing("Hỏa Long Kiếm", "Thanh kiếm rực lửa.", "WEAPON", SlotType.WEAPON, Rarity.LEGENDARY, 1000, "s_sword_1", 100, 0, 0);
        // Armor
        createItemIfMissing("Hắc Ám Giáp", "Giáp từ vực thẳm.", "ARMOR", SlotType.ARMOR, Rarity.LEGENDARY, 1000, "a_armor_1", 0, 50, 200);
        // Helmet
        createItemIfMissing("Chiến Thần Mão", "Mũ của chiến thần.", "ARMOR", SlotType.HELMET, Rarity.LEGENDARY, 800, "h_helmet_1", 0, 30, 100);
        // Boots
        createItemIfMissing("Hỏa Vân Hài", "Đi mây về gió.", "ARMOR", SlotType.BOOTS, Rarity.LEGENDARY, 800, "b_boot_1", 0, 10, 50);
        // Ring
        createItemIfMissing("Nhẫn Rồng", "Tăng sức mạnh.", "ACCESSORY", SlotType.RING, Rarity.RARE, 1500, "ri_ring_1", 50, 0, 0);
        // Necklace
        createItemIfMissing("Vòng Cổ Ngọc", "Tăng sinh lực.", "ACCESSORY", SlotType.NECKLACE, Rarity.RARE, 1500, "n_neck_1", 0, 0, 300);
        // Potion
        createItemIfMissing("Bình Máu Nhỏ", "Hồi 50 HP", "CONSUMABLE", SlotType.NONE, Rarity.COMMON, 20, "https://cdn-icons-png.flaticon.com/512/863/863816.png", 0, 0, 0);
    }

    // [FIX] Cập nhật hàm này nhận Enum SlotType và Rarity thay vì String
    private void createItemIfMissing(String name, String desc, String type, SlotType slotType, Rarity rarity, int price, String imgCode, int atk, int def, int hp) {
        if (itemRepo.findByName(name).isEmpty()) {
            Item i = new Item();
            i.setName(name);
            i.setDescription(desc);
            i.setType(type); // Loại chung (WEAPON, ARMOR...)
            i.setSlotType(slotType); // Slot cụ thể (HELMET, BOOTS...)
            i.setTier(1); // Mặc định Tier 1
            i.setRarityDefault(rarity); // Mặc định rarity
            i.setBasePrice(new BigDecimal(price));
            i.setImageUrl(imgCode);
            i.setIsSystemItem(true);

            // Base Stats (Hiển thị shop)
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
                Item itemBase = itemOpt.get();
                UserItem ui = new UserItem();
                ui.setUser(user);
                ui.setItem(itemBase);
                ui.setQuantity(1);
                ui.setIsEquipped(true); // Mặc luôn
                ui.setEnhanceLevel(10); // Cường hóa +10
                ui.setAcquiredAt(LocalDateTime.now());

                // [QUAN TRỌNG] Set Rarity & Main Stat & Sub Stats để Admin không bị lỗi
                ui.setRarity(itemBase.getRarityDefault());

                // Fake Main Stat
                if (itemBase.getSlotType() == SlotType.WEAPON) {
                    ui.setMainStatType("ATK_FLAT"); ui.setMainStatValue(new BigDecimal(500));
                } else if (itemBase.getSlotType() == SlotType.BOOTS) {
                    ui.setMainStatType("SPEED"); ui.setMainStatValue(new BigDecimal(20));
                } else {
                    ui.setMainStatType("HP_FLAT"); ui.setMainStatValue(new BigDecimal(1000));
                }

                // Fake Sub Stats (JSON) cho Admin mạnh bá đạo
                String adminGodModeStats = "[{\"type\":\"CRIT_RATE\",\"value\":100},{\"type\":\"CRIT_DMG\",\"value\":300},{\"type\":\"ATK_PERCENT\",\"value\":50},{\"type\":\"SPEED\",\"value\":20}]";
                ui.setSubStats(adminGodModeStats);

                userItemRepo.save(ui);
            }
        }
    }
}