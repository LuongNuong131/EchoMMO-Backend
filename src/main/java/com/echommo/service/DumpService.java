package com.echommo.service;

import com.echommo.dto.DumpRequest;
import com.echommo.dto.DumpResponse;
import com.echommo.entity.*;
import com.echommo.entity.Character;
import com.echommo.enums.Rarity;
import com.echommo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DumpService {

    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final UserItemRepository userItemRepository;
    private final WalletRepository walletRepository;
    private final ItemRepository itemRepository;

    private final Random random = new Random();

    @Transactional
    public DumpResponse processDump(String username, DumpRequest request) {
        long startTime = System.currentTimeMillis();

        // 1. Validate
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Character character = characterRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Character not found"));

        int amount = request.getAmount();
        if (amount <= 0) throw new RuntimeException("Số lượng không hợp lệ");

        // 2. Config tỷ lệ
        String inputItemCode;
        double megalodonRate;
        String type = request.getType().toUpperCase();
        switch (type) {
            case "NORMAL": inputItemCode = "f_fish"; megalodonRate = 0.1; break;
            case "SHARK": inputItemCode = "f_shark"; megalodonRate = 2.5; break;
            case "WHITE_SHARK": inputItemCode = "f_whiteshark"; megalodonRate = 10.0; break;
            default: throw new RuntimeException("Loại cá không hợp lệ!");
        }

        // 3. Trừ item đầu vào
        Item inputItem = itemRepository.findByCode(inputItemCode)
                .orElseThrow(() -> new RuntimeException("Item code " + inputItemCode + " không tồn tại"));
        UserItem userItem = userItemRepository.findByCharacter_CharIdAndItem_ItemId(character.getCharId(), inputItem.getItemId())
                .orElseThrow(() -> new RuntimeException("Bạn không sở hữu " + inputItem.getName()));

        if (userItem.getQuantity() < amount) throw new RuntimeException("Không đủ số lượng!");

        userItem.setQuantity(userItem.getQuantity() - amount);
        if (userItem.getQuantity() <= 0) userItemRepository.delete(userItem);
        else userItemRepository.save(userItem);

        // 4. Pre-fetch
        Item megalodonItem = itemRepository.findByCode("f_megalodon").orElse(null);
        List<Item> commonItems = getCachedItemsByRarity(Rarity.COMMON);
        List<Item> rareItems = getCachedItemsByRarity(Rarity.RARE);
        List<Item> epicItems = getCachedItemsByRarity(Rarity.EPIC);

        // 5. Logic random (giữ nguyên)
        BigDecimal totalGold = BigDecimal.ZERO;
        BigDecimal totalEcho = BigDecimal.ZERO;
        Map<String, Integer> rewardMap = new HashMap<>();

        for (int i = 0; i < amount; i++) {
            if (megalodonItem != null && (random.nextDouble() * 100) < megalodonRate) {
                rewardMap.merge(megalodonItem.getCode(), 1, Integer::sum);
                continue;
            }
            int roll = random.nextInt(100);
            if (roll < 70) {
                totalGold = totalGold.add(BigDecimal.valueOf(random.nextInt(11) + 10));
            } else if (roll < 90) {
                Item junk = getRandomItem(commonItems);
                if (junk != null) rewardMap.merge(junk.getCode(), 1, Integer::sum);
                else totalGold = totalGold.add(BigDecimal.valueOf(5));
            } else if (roll < 99) {
                Item rare = getRandomItem(random.nextBoolean() ? rareItems : epicItems);
                if (rare != null) rewardMap.merge(rare.getCode(), 1, Integer::sum);
                else totalGold = totalGold.add(BigDecimal.valueOf(50));
            } else {
                totalEcho = totalEcho.add(BigDecimal.valueOf(0.001 + (0.499 * random.nextDouble())));
            }
        }

        // 6. GHI DB & BUILD RESPONSE
        List<DumpResponse.RewardItem> responseItems = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : rewardMap.entrySet()) {
            String code = entry.getKey();
            int qty = entry.getValue();

            Item item = itemRepository.findByCode(code).orElse(null);
            if (item == null) continue;

            Optional<UserItem> existing = userItemRepository.findByCharacter_CharIdAndItem_ItemId(character.getCharId(), item.getItemId());
            if (existing.isPresent()) {
                UserItem ui = existing.get();
                ui.setQuantity(ui.getQuantity() + qty);
                userItemRepository.save(ui);
            } else {
                UserItem newItem = new UserItem();
                newItem.setCharacter(character);
                newItem.setItem(item);
                newItem.setQuantity(qty);
                newItem.setIsEquipped(false);
                userItemRepository.save(newItem);
            }

            // [UPDATED] Thêm trường image lấy từ item.getImageUrl()
            responseItems.add(DumpResponse.RewardItem.builder()
                    .code(item.getCode())
                    .name(item.getName())
                    .image(item.getImageUrl()) // Quan trọng: Lấy link ảnh gốc từ DB
                    .rarity(item.getRarity())
                    .quantity(qty)
                    .build());
        }

        // 7. Update Wallet (giữ nguyên)
        if (totalGold.compareTo(BigDecimal.ZERO) > 0 || totalEcho.compareTo(BigDecimal.ZERO) > 0) {
            Wallet wallet = walletRepository.findByUser(user).orElseThrow();
            if (totalGold.compareTo(BigDecimal.ZERO) > 0) wallet.setGold(wallet.getGold().add(totalGold));
            if (totalEcho.compareTo(BigDecimal.ZERO) > 0) wallet.setEchoCoin(wallet.getEchoCoin().add(totalEcho));
            walletRepository.save(wallet);
        }

        System.out.println("Dump " + amount + " items took: " + (System.currentTimeMillis() - startTime) + "ms");

        return DumpResponse.builder()
                .success(true)
                .message("Đã thả " + amount + " " + inputItem.getName())
                .totalGold(totalGold)
                .totalEcho(totalEcho)
                .receivedItems(responseItems)
                .build();
    }

    private List<Item> getCachedItemsByRarity(Rarity rarity) {
        List<Item> items = itemRepository.findByRarity(rarity);
        if (items == null) return new ArrayList<>();
        return items.stream().filter(i -> !i.getCode().startsWith("f_")).collect(Collectors.toList());
    }

    private Item getRandomItem(List<Item> items) {
        if (items == null || items.isEmpty()) return null;
        return items.get(random.nextInt(items.size()));
    }
}