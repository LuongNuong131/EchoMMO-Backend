package com.echommo.service;

import com.echommo.dto.ExplorationResponse;
import com.echommo.entity.*;
import com.echommo.entity.Character;
import com.echommo.enums.CharacterStatus;
import com.echommo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class ExplorationService {
    @Autowired private CharacterService characterService;
    @Autowired private CharacterRepository characterRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CaptchaService captchaService;

    // [NEW] Inject thêm các Repo cần thiết cho logic mới
    @Autowired private ItemRepository itemRepo;
    @Autowired private UserItemRepository userItemRepo;
    @Autowired private FlavorTextRepository flavorTextRepo;
    @Autowired private WeatherTextRepository weatherTextRepo;

    // --- 1. TÍNH NĂNG THÁM HIỂM (HÀNH TẨU) ---
    @Transactional
    public ExplorationResponse explore() {
        try {
            Character c = characterService.getMyCharacter();

            // [AUTO-CREATE] Nếu chưa có nhân vật, tạo ngay
            if (c == null) {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("User not found in DB"));
                c = characterService.createDefaultCharacter(user);
            }
            if (c == null) throw new RuntimeException("Lỗi dữ liệu nhân vật.");

            // Check Anti-cheat
            captchaService.checkLockStatus(c.getUser());

            // [UPDATE] Bỏ logic trừ Energy (Grinding Free)
            // if (c.getEnergy() < 2) { ... } -> Đã xóa

            Random r = new Random();

            // --- A. PHẦN THƯỞNG CỐ ĐỊNH (BASE REWARD) ---
            int baseExp = 1 + (c.getLv() / 2);
            int baseCoin = 1 + r.nextInt(3); // 1-3 vàng

            c.setExp(c.getExp() + baseExp);

            Wallet w = c.getUser().getWallet();
            BigDecimal currentGold = w.getGold() != null ? w.getGold() : BigDecimal.ZERO;
            w.setGold(currentGold.add(BigDecimal.valueOf(baseCoin)));

            // --- B. XỬ LÝ RNG (Sự kiện ngẫu nhiên 45/10/20/25) ---
            int roll = r.nextInt(100); // 0 - 99
            String type;
            String msg;
            String rewardName = null;
            Integer rewardAmount = 0;
            BigDecimal eventGold = BigDecimal.ZERO;

            if (roll < 45) {
                // 45%: Flavor Text / Weather
                type = "TEXT";
                boolean isWeather = r.nextBoolean();
                if (isWeather) {
                    msg = weatherTextRepo.findRandomContent().orElse("Gió thổi hiu hiu, lá bay xào xạc...");
                } else {
                    msg = flavorTextRepo.findRandomContent().orElse("Giang hồ hiểm ác, cẩn thận củi lửa.");
                }

            } else if (roll < 55) {
                // 10%: Big Gold
                type = "GOLD";
                int bigGold = 10 + r.nextInt(41); // 10 - 50 vàng
                eventGold = BigDecimal.valueOf(bigGold);
                w.setGold(w.getGold().add(eventGold));
                msg = "Bạn đá phải một cái túi nặng trịch. Bên trong là " + bigGold + " Vàng!";
                rewardName = "Túi vàng";
                rewardAmount = bigGold;

            } else if (roll < 75) {
                // 20%: Item Drop
                type = "ITEM";
                Item droppedItem = getResourceByMapLevel(c.getLv(), r);

                if (droppedItem != null) {
                    addItemToInventory(c.getUser(), droppedItem, 1);
                    msg = "Bạn tìm thấy " + droppedItem.getName() + "!";
                    rewardName = droppedItem.getName();
                    rewardAmount = 1;
                } else {
                    type = "TEXT";
                    msg = "Bạn thấy lấp lánh nhưng chỉ là hòn đá cuội.";
                }

            } else {
                // 25%: Combat
                type = "COMBAT";
                msg = "Sát khí đằng đằng! Quái vật xuất hiện.";
                c.setStatus(CharacterStatus.IN_COMBAT);
            }

            // --- C. CHECK LÊN CẤP ---
            Integer newLv = null;
            long reqExp = (long) c.getLv() * 100L;
            if (c.getExp() >= reqExp) {
                c.setExp(c.getExp() - (int) reqExp);
                c.setLv(c.getLv() + 1);
                c.setMaxHp(c.getMaxHp() + 20);
                c.setHp(c.getMaxHp());
                c.setEnergy(c.getMaxEnergy());
                newLv = c.getLv();
                msg += " [LÊN CẤP ĐỘ " + newLv + "!]";
            }

            // --- D. LƯU & TRẢ VỀ ---
            characterRepository.save(c);
            walletRepository.save(w);

            return new ExplorationResponse(
                    msg,
                    type,
                    BigDecimal.valueOf(baseCoin).add(eventGold),
                    c.getExp(),
                    c.getLv(),
                    c.getEnergy(),
                    c.getMaxEnergy(),
                    newLv,
                    rewardName,
                    rewardAmount
            );

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi thám hiểm: " + e.getMessage());
        }
    }

    // --- 2. TÍNH NĂNG KHAI THÁC (GATHERING - GIỮ LẠI ĐỂ KHÔNG LỖI) ---
    @Transactional
    public Map<String, Object> gatherResource(String resourceType, int amount) {
        Character c = characterService.getMyCharacter();
        if (c == null) throw new RuntimeException("Chưa có nhân vật");

        // 1. Tính toán tiêu hao (1 Năng lượng / 1 lần khai thác)
        int energyCost = amount;
        if (c.getEnergy() < energyCost) {
            throw new RuntimeException("Không đủ nội năng! Cần " + energyCost);
        }

        // 2. Trừ năng lượng
        c.setEnergy(c.getEnergy() - energyCost);
        characterRepository.save(c);

        // 3. Cộng tài nguyên vào ví
        Wallet w = c.getUser().getWallet();
        String msg = "";

        switch (resourceType) {
            case "wood": // Cây Sồi
            case "special": // Gỗ Hóa Thạch
                w.setWood(w.getWood() + amount);
                msg = "Nhận được " + amount + " Gỗ";
                break;
            case "stone": // Đá Tảng
            case "mining": // Mỏ Đồng
                w.setStone(w.getStone() + amount);
                msg = "Nhận được " + amount + " Đá";
                break;
            default:
                throw new RuntimeException("Loại tài nguyên không hợp lệ");
        }

        walletRepository.save(w);

        // 4. Trả về kết quả
        Map<String, Object> result = new HashMap<>();
        result.put("message", msg);
        result.put("currentEnergy", c.getEnergy());
        result.put("wood", w.getWood());
        result.put("stone", w.getStone());

        return result;
    }

    // --- CÁC HÀM PHỤ TRỢ ---

    private Item getResourceByMapLevel(int level, Random r) {
        List<String> possibleItems = new ArrayList<>();
        possibleItems.add("Gỗ");
        possibleItems.add("Đá");

        if (level >= 10) possibleItems.add("Quặng Đồng");
        if (level >= 20) possibleItems.add("Quặng Sắt");
        if (level >= 30) possibleItems.add("Bạch Kim");

        // Chọn item ngẫu nhiên từ list
        String itemName = possibleItems.get(r.nextInt(possibleItems.size()));

        // Tìm item trong DB
        return itemRepo.findByName(itemName).stream().findFirst().orElse(null);
    }

    private void addItemToInventory(User user, Item item, int amount) {
        UserItem userItem = userItemRepo.findByUser_UserId(user.getUserId())
                .stream()
                .filter(ui -> ui.getItem().getItemId().equals(item.getItemId()))
                .findFirst()
                .orElse(new UserItem());

        if (userItem.getUserItemId() == null) {
            userItem.setUser(user);
            userItem.setItem(item);
            userItem.setQuantity(amount);
            userItem.setIsEquipped(false);
        } else {
            userItem.setQuantity(userItem.getQuantity() + amount);
        }
        userItemRepo.save(userItem);
    }
}