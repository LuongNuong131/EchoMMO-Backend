package com.echommo.config;

import com.echommo.entity.Character;
import com.echommo.entity.Item;
import com.echommo.enums.Rarity;
import com.echommo.enums.SlotType;
import com.echommo.repository.CharacterRepository;
import com.echommo.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private ItemRepository itemRepo;
    @Autowired private CharacterRepository charRepo;

    @Override
    public void run(String... args) throws Exception {
//        createMissingItems();
        healAllCharacters();
    }

    private void healAllCharacters() {
        System.out.println(">>> Checking & Healing characters...");
        List<Character> chars = charRepo.findAll();
        for (Character c : chars) {
            boolean changed = false;
            if (c.getMaxHp() == null || c.getMaxHp() == 0) { c.setMaxHp(100); changed = true; }
            if (c.getMaxEnergy() == null || c.getMaxEnergy() == 0) { c.setMaxEnergy(50); changed = true; }

            if (c.getCurrentHp() == null || c.getCurrentHp() <= 0) {
                c.setCurrentHp(c.getMaxHp());
                changed = true;
            }
            if (c.getCurrentEnergy() == null || c.getCurrentEnergy() <= 0) {
                c.setCurrentEnergy(c.getMaxEnergy());
                changed = true;
            }

            if (changed) {
                charRepo.save(c);
                System.out.println(">>> Đã hồi phục cho nhân vật: " + c.getName());
            }
        }
    }
    private void createMissingItems() {
        // [FIX] Truyền đầy đủ mã CODE vào hàm khởi tạo
        createIfMissing("w_wood", "Gỗ Sồi", "MATERIAL", Rarity.COMMON, 1, "w_wood.png", SlotType.MATERIAL);
        createIfMissing("o_coal", "Than", "MATERIAL", Rarity.COMMON, 1, "o_coal.png", SlotType.MATERIAL);
        createIfMissing("o_copper", "Quặng Đồng", "MATERIAL", Rarity.COMMON, 1, "o_copper.png", SlotType.MATERIAL);
        createIfMissing("o_iron", "Sắt", "MATERIAL", Rarity.RARE, 2, "o_iron.png", SlotType.MATERIAL);
        createIfMissing("f_fish", "Cá", "MATERIAL", Rarity.COMMON, 1, "f_fish.png", SlotType.MATERIAL);
        createIfMissing("s_sword_0", "Kiếm Gỗ", "WEAPON", Rarity.COMMON, 1, "s_sword_0.png", SlotType.WEAPON);
        createIfMissing("r_potion", "Bình Máu Nhỏ", "CONSUMABLE", Rarity.COMMON, 1, "r_potion.png", SlotType.CONSUMABLE);
    }

    private void createIfMissing(String code, String name, String type, Rarity rarity, int tier, String img, SlotType slot) {
        // [FIX] Kiểm tra theo CODE duy nhất
        if (itemRepo.findByCode(code).isPresent()) return;

        Item item = new Item();
        item.setCode(code); // QUAN TRỌNG: Gán code để không bị lỗi DB
        item.setName(name);
        item.setType(type);
        item.setRarity(rarity);
        item.setTier(tier);
        item.setImageUrl(img);
        item.setSlotType(slot);
        item.setBasePrice(BigDecimal.valueOf(10));
        item.setDescription("Vật phẩm hệ thống.");

        itemRepo.save(item);
        System.out.println(">>> Đã bổ sung item: [" + code + "] " + name);
    }
}