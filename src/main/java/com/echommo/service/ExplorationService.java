package com.echommo.service;

import com.echommo.dto.ExplorationResponse;
import com.echommo.entity.Character;
import com.echommo.entity.User;
import com.echommo.entity.Wallet;
import com.echommo.repository.CharacterRepository;
import com.echommo.repository.UserRepository;
import com.echommo.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Random;

@Service
public class ExplorationService {

    @Autowired private CharacterService characterService;
    @Autowired private CharacterRepository characterRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CaptchaService captchaService;

    private final Random random = new Random();

    // Hằng số tính EXP Level Up (Đồng bộ với GameService)
    private static final long BASE_EXP_REQUIREMENT = 100L;

    @Transactional
    public ExplorationResponse explore() {
        Character character = characterService.getMyCharacter();
        User user = character.getUser();

        // 1. Check Captcha
        captchaService.checkLockStatus(user);
        if (Boolean.TRUE.equals(user.getIsCaptchaLocked())) {
            throw new RuntimeException("CAPTCHA");
        }
        if (random.nextInt(100) < 1) { // 1% dính Captcha
            user.setIsCaptchaLocked(true);
            userRepository.save(user);
            throw new RuntimeException("CAPTCHA");
        }

        // 2. Trừ Energy (Fixed Flow)
        // FIX 4: Nếu Energy bằng 0, cho nghỉ ngơi (+1 Energy)
        if (character.getEnergy() == 0) {
            character.setEnergy(1);
            characterRepository.save(character);
            // LƯU Ý: Frontend cần được cập nhật để xử lý Response này
            return new ExplorationResponse("Bạn nghỉ ngơi một chút... (+1 ⚡)", "REST", BigDecimal.ZERO, character.getExp(), character.getLv(), character.getEnergy(), character.getMaxEnergy(), null);
        }
        // Trừ 1 Energy cho hành động khám phá
        character.setEnergy(character.getEnergy() - 1);

        // 3. RNG Logic
        int roll = random.nextInt(100);
        String message;
        String type;
        BigDecimal gold = BigDecimal.ZERO;
        Long expGain = 5L;
        Integer newLevel = null;
        Wallet wallet = user.getWallet();

        if (roll < 15) {
            // 15% Gặp Quái
            type = "ENEMY";
            message = "Có tiếng động lạ trong bụi cỏ!";
            expGain = 0L; // Gặp quái chưa có exp
        } else if (roll < 45) {
            // 30% Nhặt Vàng
            type = "GOLD";
            int amount = 10 + random.nextInt(20);
            gold = new BigDecimal(amount);
            message = "Bạn nhặt được " + amount + " vàng rơi trên đường.";
        } else if (roll < 55) {
            // 10% Sự kiện lạ
            type = "EVENT";
            String[] events = {"Bạn thấy một đám mây hình con vịt.", "Gió thổi mát quá.", "Bạn vấp phải cục đá."};
            message = events[random.nextInt(events.length)];
        } else {
            // Đi bộ bình thường
            type = "WALK";
            message = "Bạn bước đi trên con đường mòn...";
        }

        // 4. Cộng thưởng và Level Up
        if (gold.compareTo(BigDecimal.ZERO) > 0) {
            wallet.setGold(wallet.getGold().add(gold));
            walletRepository.save(wallet);
        }

        if (expGain > 0) {
            // FIX 1 & 3: Sử dụng getExp/setExp và tính toán EXP
            character.setExp(character.getExp() + expGain.intValue());

            // Tính EXP yêu cầu cho Level hiện tại
            long requiredExp = BASE_EXP_REQUIREMENT * (long) Math.pow(character.getLv(), 2);

            if (character.getExp() >= requiredExp) {
                // FIX 1 & 2: Dùng setLv/getLv và setBaseAtk (hoặc setAtk nếu Entity không dùng Base)
                character.setExp(character.getExp() - (int) requiredExp);
                character.setLv(character.getLv() + 1);

                // Tăng chỉ số cơ bản khi lên cấp (Dùng setBaseAtk/setBaseDef)
                character.setBaseAtk(character.getBaseAtk() + 5);
                character.setBaseDef(character.getBaseDef() + 2);

                character.setMaxHp(character.getMaxHp() + 50);
                character.setHp(character.getMaxHp());
                character.setEnergy(character.getMaxEnergy());

                newLevel = character.getLv();
                message += " [LÊN CẤP!]";
            }
        }

        characterRepository.save(character);

        // FIX 5: Trả về EXP và Level hiện tại
        return new ExplorationResponse(message, type, gold, character.getExp(), character.getLv(), character.getEnergy(), character.getMaxEnergy(), newLevel);
    }
}