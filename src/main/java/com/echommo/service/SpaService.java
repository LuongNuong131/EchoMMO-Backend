package com.echommo.service;

import com.echommo.dto.SpaStatusResponse;
import com.echommo.entity.Character;
import com.echommo.entity.User;
import com.echommo.entity.Wallet;
import com.echommo.enums.SpaPackage;
import com.echommo.repository.CharacterRepository;
import com.echommo.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SpaService {
    private final WalletRepository walletRepository;
    private final CharacterRepository characterRepository;

    @Transactional
    public SpaStatusResponse useSpa(User user, String packageType) {
        Character character = characterRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Character not found"));

        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        // [FIX] Check null ngay lập tức để tránh NullPointerException khi unboxing
        if (character.getDailySpaUsage() == null) {
            character.setDailySpaUsage(0);
        }

        // 1. Kiểm tra Reset Daily (Reset số lần dùng nếu sang ngày mới)
        LocalDate today = LocalDate.now();
        if (character.getLastFreeSpaUse() == null ||
                !character.getLastFreeSpaUse().toLocalDate().equals(today)) {
            character.setDailySpaUsage(0);
        }

        // 2. Kiểm tra đang Spa không
        if (character.getSpaEndTime() != null && character.getSpaEndTime().isAfter(LocalDateTime.now())) {
            long secondsRemaining = java.time.Duration.between(LocalDateTime.now(), character.getSpaEndTime()).getSeconds();
            return new SpaStatusResponse(
                    "Đang thư giãn... còn " + secondsRemaining + "s",
                    character.getCurrentHp(),
                    character.getCurrentEnergy(),
                    wallet.getGold(),
                    wallet.getEchoCoin()
            );
        }

        SpaPackage pack;
        try {
            pack = SpaPackage.valueOf(packageType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Gói Spa không hợp lệ!");
        }

        BigDecimal cost = BigDecimal.ZERO;
        boolean isFree = false;
        int duration = pack.getDurationSeconds(); // BASIC=120, VIP=10

        // 3. Logic Tính Tiền & Hồi Phục
        if (pack == SpaPackage.BASIC) {
            // --- GÓI THƯỜNG (BASIC) ---
            // Hồi 50% Max HP/Energy
            int hpRecover = character.getMaxHp() / 2;
            int energyRecover = character.getMaxEnergy() / 2;

            // Logic Free 2 lần/ngày
            // Vì đã check null ở trên nên dòng này an toàn tuyệt đối
            if (character.getDailySpaUsage() < 2) {
                cost = BigDecimal.ZERO;
                isFree = true;
            } else {
                // Hết free -> Tính tiền: Level * 100 Gold
                cost = new BigDecimal(character.getLevel() * 100);
            }

            // Thanh toán Gold (nếu không free)
            if (!isFree) {
                if (wallet.getGold().compareTo(cost) < 0) {
                    throw new RuntimeException("Không đủ Vàng! Cần: " + cost);
                }
                wallet.setGold(wallet.getGold().subtract(cost));
            }

            // Thực hiện hồi (cộng thêm vào hiện tại, max là MaxHp)
            character.setCurrentHp(Math.min(character.getCurrentHp() + hpRecover, character.getMaxHp()));
            character.setCurrentEnergy(Math.min(character.getCurrentEnergy() + energyRecover, character.getMaxEnergy()));

            // Tăng biến đếm sử dụng
            character.setDailySpaUsage(character.getDailySpaUsage() + 1);
            character.setLastFreeSpaUse(LocalDateTime.now()); // Update timestamp để check reset ngày

        } else if (pack == SpaPackage.VIP) {
            // --- GÓI THƯỢNG HẠNG (VIP) ---
            // Hồi 100% Full
            character.setCurrentHp(character.getMaxHp());
            character.setCurrentEnergy(character.getMaxEnergy());

            // Logic giá: 0.5 + (Level * 0.05) EchoCoin
            double coinCostDouble = 0.5 + (character.getLevel() * 0.05);
            cost = BigDecimal.valueOf(coinCostDouble);

            // Thanh toán Coin
            if (wallet.getEchoCoin().compareTo(cost) < 0) {
                throw new RuntimeException("Không đủ Echo Coin! Cần: " + cost);
            }
            wallet.setEchoCoin(wallet.getEchoCoin().subtract(cost));
        }

        // 4. Set thời gian Spa
        character.setSpaStartTime(LocalDateTime.now());
        character.setSpaEndTime(LocalDateTime.now().plusSeconds(duration));
        character.setSpaPackageType(pack.name());

        // Lưu DB
        characterRepository.save(character);
        walletRepository.save(wallet);

        String msg = isFree
                ? "Thư giãn miễn phí (" + character.getDailySpaUsage() + "/2)!"
                : "Thanh toán thành công (-" + cost + (pack == SpaPackage.VIP ? " Coin)" : " Vàng)");

        return new SpaStatusResponse(
                msg,
                character.getCurrentHp(),
                character.getCurrentEnergy(),
                wallet.getGold(),
                wallet.getEchoCoin()
        );
    }
}