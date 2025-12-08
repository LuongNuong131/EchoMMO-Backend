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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    @Transactional
    public ExplorationResponse explore() {
        try {
            Character c = characterService.getMyCharacter();

            // 1. [AUTO-CREATE] Nếu chưa có nhân vật, tạo ngay
            if (c == null) {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("User not found in DB"));
                c = characterService.createDefaultCharacter(user);
            }
            if (c == null) throw new RuntimeException("Lỗi dữ liệu nhân vật.");

            // 2. Check Anti-cheat
            captchaService.checkLockStatus(c.getUser());

            // 3. [UPDATE] Bỏ logic trừ Energy (Grinding Free)
            // if (c.getEnergy() < 2) { ... } -> Đã xóa

            Random r = new Random();

            // --- A. PHẦN THƯỞNG CỐ ĐỊNH (BASE REWARD) ---
            // Luôn nhận được 1 ít Exp và Vàng lẻ mỗi bước chân
            int baseExp = 1 + (c.getLv() / 2);
            int baseCoin = 1 + r.nextInt(3); // 1-3 vàng

            // Cộng vào Character/Wallet ngay
            c.setExp(c.getExp() + baseExp);

            Wallet w = c.getUser().getWallet();
            // Cộng dồn vàng (Dùng getGold/setGold theo đúng Entity Wallet của bạn)
            BigDecimal currentGold = w.getGold() != null ? w.getGold() : BigDecimal.ZERO;
            w.setGold(currentGold.add(BigDecimal.valueOf(baseCoin)));

            // --- B. XỬ LÝ RNG (Sự kiện ngẫu nhiên 45/10/20/25) ---
            int roll = r.nextInt(100); // 0 - 99
            String type;
            String msg;
            String rewardName = null;
            Integer rewardAmount = 0;
            BigDecimal eventGold = BigDecimal.ZERO; // Vàng từ sự kiện (nếu có)

            if (roll < 45) {
                // 45%: Flavor Text / Weather (Không có quà thêm)
                type = "TEXT";
                boolean isWeather = r.nextBoolean(); // 50/50
                // Dùng orElse để tránh lỗi nếu DB chưa có dữ liệu text
                if (isWeather) {
                    msg = weatherTextRepo.findRandomContent().orElse("Gió thổi hiu hiu, lá bay xào xạc...");
                } else {
                    msg = flavorTextRepo.findRandomContent().orElse("Giang hồ hiểm ác, cẩn thận củi lửa.");
                }

            } else if (roll < 55) {
                // 10%: Big Gold (Túi vàng)
                type = "GOLD";
                int bigGold = 10 + r.nextInt(41); // 10 - 50 vàng
                eventGold = BigDecimal.valueOf(bigGold);

                // Cộng thêm vào ví
                w.setGold(w.getGold().add(eventGold));

                msg = "Bạn đá phải một cái túi nặng trịch. Bên trong là " + bigGold + " Vàng!";
                rewardName = "Túi vàng";
                rewardAmount = bigGold;

            } else if (roll < 75) {
                // 20%: Item Drop (Tài nguyên theo Map)
                type = "ITEM";
                Item droppedItem = getResourceByMapLevel(c.getLv(), r);

                if (droppedItem != null) {
                    addItemToInventory(c.getUser(), droppedItem, 1);
                    msg = "Bạn tìm thấy " + droppedItem.getName() + "!";
                    rewardName = droppedItem.getName();
                    rewardAmount = 1;
                } else {
                    type = "TEXT"; // Fallback nếu không tìm thấy item trong DB
                    msg = "Bạn thấy lấp lánh nhưng chỉ là hòn đá cuội.";
                }

            } else {
                // 25%: Combat (Gặp quái)
                type = "COMBAT";
                msg = "Sát khí đằng đằng! Quái vật xuất hiện.";
                c.setStatus(CharacterStatus.IN_COMBAT);
                // Frontend sẽ tự chuyển trang khi thấy type = COMBAT
            }

            // --- C. CHECK LÊN CẤP ---
            Integer newLv = null;
            // Công thức Exp: Lv * 100 (Dễ thở hơn)
            long reqExp = (long) c.getLv() * 100L;
            if (c.getExp() >= reqExp) {
                c.setExp(c.getExp() - (int) reqExp);
                c.setLv(c.getLv() + 1);

                // Tăng stat khi lên cấp
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
                    BigDecimal.valueOf(baseCoin).add(eventGold), // Tổng vàng hiển thị
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

    // --- CÁC HÀM PHỤ TRỢ ---

    private Item getResourceByMapLevel(int level, Random r) {
        List<String> possibleItems = new ArrayList<>();
        possibleItems.add("Gỗ"); // Item cơ bản
        possibleItems.add("Đá");

        // Logic Drop theo Level (Map giả định)
        if (level >= 10) possibleItems.add("Quặng Đồng");
        if (level >= 20) possibleItems.add("Quặng Sắt");
        if (level >= 30) possibleItems.add("Bạch Kim");
        // Đảm bảo tên trong DB trùng khớp chính xác với chuỗi này

        String itemName = possibleItems.get(r.nextInt(possibleItems.size()));
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