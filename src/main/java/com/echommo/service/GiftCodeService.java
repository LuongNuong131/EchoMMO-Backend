package com.echommo.service;

import com.echommo.entity.*;
import com.echommo.entity.Character;
import com.echommo.enums.SlotType;
import com.echommo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GiftCodeService {

    @Autowired private GiftCodeRepository giftCodeRepository;
    @Autowired private GiftCodeUsageRepository usageRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private UserItemRepository userItemRepository;
    @Autowired private CharacterRepository characterRepository;

    @Transactional
    public String redeemCode(String codeStr, String username) {
        long startTime = System.currentTimeMillis(); // Đo tốc độ xử lý

        // 1. Tìm User
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đại hiệp!"));

        // 2. Tìm Giftcode
        GiftCode gc = giftCodeRepository.findByCode(codeStr)
                .orElseThrow(() -> new RuntimeException("Mã quà tặng không tồn tại!"));

        // 3. Validate
        if (!Boolean.TRUE.equals(gc.getIsActive())) {
            throw new RuntimeException("Mã này đã bị vô hiệu hóa.");
        }
        if (gc.getExpirationDate() != null && gc.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Mã đã hết hạn sử dụng.");
        }
        if (usageRepository.existsByUserUserIdAndGiftCodeCode(user.getUserId(), codeStr)) {
            throw new RuntimeException("Huynh đệ đã nhập mã này rồi!");
        }

        StringBuilder msg = new StringBuilder();

        // 4. Xử lý TIỀN (Vàng/Coin)
        boolean hasMoneyReward = (gc.getGoldReward() != null && gc.getGoldReward() > 0) ||
                (gc.getCoinReward() != null && gc.getCoinReward() > 0);

        if (hasMoneyReward) {
            Wallet wallet = walletRepository.findByUser_UserId(user.getUserId()).orElse(new Wallet());
            if (wallet.getUser() == null) {
                wallet.setUser(user);
                wallet.setGold(BigDecimal.ZERO);
                wallet.setEchoCoin(BigDecimal.ZERO);
            }

            if (gc.getGoldReward() != null && gc.getGoldReward() > 0) {
                wallet.setGold(wallet.getGold().add(BigDecimal.valueOf(gc.getGoldReward())));
                msg.append("Đã nhận ").append(gc.getGoldReward()).append(" Vàng. ");
            }
            if (gc.getCoinReward() != null && gc.getCoinReward() > 0) {
                wallet.setEchoCoin(wallet.getEchoCoin().add(BigDecimal.valueOf(gc.getCoinReward())));
                msg.append("Đã nhận ").append(gc.getCoinReward()).append(" Echo Coin. ");
            }
            walletRepository.save(wallet);
        }

        // 5. Xử lý ITEM (Tối ưu tốc độ - Batch Processing)
        if ("DEV_TOOL".equals(gc.getType())) {
            Optional<Character> characterOpt = characterRepository.findByUser_UserId(user.getUserId());

            if (characterOpt.isPresent()) {
                Character targetChar = characterOpt.get();

                // A. Lấy tất cả dữ liệu cần thiết 1 lần (Bulk Fetch)
                List<Item> allItems = itemRepository.findAll();
                List<UserItem> existingUserItems = userItemRepository.findByCharacter_CharId(targetChar.getCharId());

                // B. Tạo Map để tra cứu nhanh (Thay vì query DB trong vòng lặp)
                // Map<ItemId, UserItem>
                Map<Integer, UserItem> userItemMap = existingUserItems.stream()
                        .collect(Collectors.toMap(ui -> ui.getItem().getItemId(), Function.identity()));

                List<UserItem> itemsToSave = new ArrayList<>();
                int countAdded = 0;

                // C. Xử lý Logic trong bộ nhớ (Memory) - Cực nhanh
                for (Item item : allItems) {
                    UserItem ui = userItemMap.get(item.getItemId());

                    // Nếu chưa có thì tạo mới
                    if (ui == null) {
                        ui = new UserItem();
                        ui.setCharacter(targetChar);
                        ui.setItem(item);
                        ui.setQuantity(0);
                        ui.setIsEquipped(false);
                        ui.setEnhanceLevel(0);
                        ui.setIsMythic(false);
                        ui.setVisualVariant(0);
                        // Set độ bền
                        if (item.getMaxDurability() != null) {
                            ui.setMaxDurability(item.getMaxDurability());
                            ui.setCurrentDurability(item.getMaxDurability());
                        } else {
                            ui.setMaxDurability(100);
                            ui.setCurrentDurability(100);
                        }
                    }

                    // Logic cộng số lượng
                    SlotType type = item.getSlotType();
                    if (type == SlotType.MATERIAL || type == SlotType.CONSUMABLE) {
                        ui.setQuantity(ui.getQuantity() + 10000);
                    } else {
                        // Tool/Equip: Chỉ thêm nếu chưa có (quantity <= 0)
                        if (ui.getQuantity() <= 0) {
                            ui.setQuantity(1);
                        }
                    }

                    itemsToSave.add(ui);
                    countAdded++;
                }

                // D. Lưu tất cả 1 lần (Batch Save)
                if (!itemsToSave.isEmpty()) {
                    userItemRepository.saveAll(itemsToSave);
                }

                msg.append("Đã xử lý ").append(countAdded).append(" vật phẩm cho: ").append(targetChar.getName());
            }
        }

        // 6. Lưu lịch sử
        GiftCodeUsage usage = new GiftCodeUsage();
        usage.setUser(user);
        usage.setGiftCode(gc);
        usage.setUsedAt(LocalDateTime.now());
        usageRepository.save(usage);

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Redeem Code Time: " + duration + "ms"); // Log thời gian xử lý để kiểm tra

        if (msg.length() == 0) return "Nhập mã thành công (Không có quà).";
        return msg.toString();
    }
}