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

    @Autowired private ItemRepository itemRepo;
    @Autowired private UserItemRepository userItemRepo;
    @Autowired private FlavorTextRepository flavorTextRepo;
    @Autowired private WeatherTextRepository weatherTextRepo;

    // --- 1. TÍNH NĂNG THÁM HIỂM (HÀNH TẨU) ---
    @Transactional
    public ExplorationResponse explore() {
        try {
            Character c = characterService.getMyCharacter();

            if (c == null) {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("User not found in DB"));
                c = characterService.createDefaultCharacter(user);
            }
            if (c == null) throw new RuntimeException("Lỗi dữ liệu nhân vật.");

            captchaService.checkLockStatus(c.getUser());

            Random r = new Random();

            // --- A. PHẦN THƯỞNG CỐ ĐỊNH ---
            // [FIX] getLv() -> getLevel()
            int baseExp = 1 + (c.getLevel() / 2);
            int baseCoin = 1 + r.nextInt(3);

            // [FIX] setExp/getExp -> setCurrentExp
            c.setCurrentExp(c.getCurrentExp() + baseExp);

            Wallet w = c.getUser().getWallet();
            BigDecimal currentGold = w.getGold() != null ? w.getGold() : BigDecimal.ZERO;
            w.setGold(currentGold.add(BigDecimal.valueOf(baseCoin)));

            // --- B. XỬ LÝ RNG ---
            int roll = r.nextInt(100);
            String type;
            String msg;
            String rewardName = null;
            Integer rewardAmount = 0;
            BigDecimal eventGold = BigDecimal.ZERO;

            if (roll < 45) {
                // 45%: Flavor Text
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
                int bigGold = 10 + r.nextInt(41);
                eventGold = BigDecimal.valueOf(bigGold);
                w.setGold(w.getGold().add(eventGold));
                msg = "Bạn đá phải một cái túi nặng trịch. Bên trong là " + bigGold + " Vàng!";
                rewardName = "Túi vàng";
                rewardAmount = bigGold;

            } else if (roll < 75) {
                // 20%: Item Drop
                type = "ITEM";
                // [FIX] getLv -> getLevel
                Item droppedItem = getResourceByMapLevel(c.getLevel(), r);

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
            // [FIX] getLv -> getLevel
            long reqExp = (long) c.getLevel() * 100L;

            // [FIX] getExp -> getCurrentExp
            if (c.getCurrentExp() >= reqExp) {
                c.setCurrentExp(c.getCurrentExp() - (int) reqExp);
                c.setLevel(c.getLevel() + 1);
                c.setMaxHp(c.getMaxHp() + 20);

                // [FIX] setHp -> setCurrentHp
                c.setCurrentHp(c.getMaxHp());
                // [FIX] setEnergy -> setCurrentEnergy
                c.setCurrentEnergy(c.getMaxEnergy());

                newLv = c.getLevel();
                msg += " [LÊN CẤP ĐỘ " + newLv + "!]";
            }

            characterRepository.save(c);
            walletRepository.save(w);

            return new ExplorationResponse(
                    msg,
                    type,
                    BigDecimal.valueOf(baseCoin).add(eventGold),
                    c.getCurrentExp(), // [FIX]
                    c.getLevel(),      // [FIX]
                    c.getCurrentEnergy(), // [FIX]
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

    // --- 2. TÍNH NĂNG KHAI THÁC ---
    @Transactional
    public Map<String, Object> gatherResource(String resourceType, int amount) {
        Character c = characterService.getMyCharacter();
        if (c == null) throw new RuntimeException("Chưa có nhân vật");

        int energyCost = amount;

        // [FIX] getEnergy -> getCurrentEnergy
        if (c.getCurrentEnergy() < energyCost) {
            throw new RuntimeException("Không đủ nội năng! Cần " + energyCost);
        }

        // [FIX] setEnergy/getEnergy -> setCurrentEnergy
        c.setCurrentEnergy(c.getCurrentEnergy() - energyCost);
        characterRepository.save(c);

        Wallet w = c.getUser().getWallet();
        String msg = "";

        switch (resourceType) {
            case "wood":
            case "special":
                w.setWood(w.getWood() + amount);
                msg = "Nhận được " + amount + " Gỗ";
                break;
            case "stone":
            case "mining":
                w.setStone(w.getStone() + amount);
                msg = "Nhận được " + amount + " Đá";
                break;
            default:
                throw new RuntimeException("Loại tài nguyên không hợp lệ");
        }

        walletRepository.save(w);

        Map<String, Object> result = new HashMap<>();
        result.put("message", msg);
        result.put("currentEnergy", c.getCurrentEnergy()); // [FIX]
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

        String itemName = possibleItems.get(r.nextInt(possibleItems.size()));
        return itemRepo.findByName(itemName).stream().findFirst().orElse(null);
    }

    private void addItemToInventory(User user, Item item, int amount) {
        // [FIX] Đổi Integer -> Long cho UserItemId (nếu cần) nhưng ở đây đang dùng findByUser (List)
        // Logic dưới đây là tìm xem đã có slot item đó chưa để cộng dồn
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