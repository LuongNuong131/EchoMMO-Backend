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
    @Autowired private QuestService questService;

    private final Random random = new Random();

    @Transactional
    public ExplorationResponse explore() {
        Character character = characterService.getMyCharacter();
        User user = character.getUser();

        // 1. Check Captcha
        captchaService.checkLockStatus(user);
        if (Boolean.TRUE.equals(user.getIsCaptchaLocked())) {
            throw new RuntimeException("CAPTCHA");
        }
        if (random.nextInt(100) < 1) { // 1% tỷ lệ dính captcha
            user.setIsCaptchaLocked(true);
            userRepository.save(user);
            throw new RuntimeException("CAPTCHA");
        }

        // 2. Trừ Energy
        if (character.getEnergy() < 1) {
            // Auto rest nếu hết energy (Optional feature)
            character.setEnergy(1);
            characterRepository.save(character);
            return new ExplorationResponse("Bạn nghỉ mệt một chút... (+1 ⚡)", "REST", BigDecimal.ZERO, character.getExp(), character.getLv(), character.getEnergy(), character.getMaxEnergy(), null);
        }
        character.setEnergy(character.getEnergy() - 1);

        // Quest Progress
        questService.updateProgress(user, "EXPLORE", 1);

        // 3. RNG Event
        int roll = random.nextInt(100);
        String message;
        String type;
        BigDecimal gold = BigDecimal.ZERO;
        Long expGain = 5L;
        Integer newLevel = null;
        Wallet wallet = user.getWallet();

        if (roll < 40) {
            // 40% Nothing
            type = "NOTHING";
            String[] msgs = {"Khu rừng yên tĩnh...", "Chỉ có tiếng gió vi vu.", "Không phát hiện gì cả."};
            message = msgs[random.nextInt(msgs.length)];
        } else if (roll < 70) {
            // 30% Gold
            type = "GOLD";
            int amount = 10 + random.nextInt(21); // 10-30 gold
            gold = BigDecimal.valueOf(amount);
            message = "Bạn tìm thấy " + amount + " vàng rơi trên đường!";

            wallet.setGold(wallet.getGold().add(gold));
            walletRepository.save(wallet);
        } else {
            // 30% Enemy
            type = "ENEMY";
            message = "Có sát khí! Quái vật xuất hiện!";
            expGain = 0L; // Gặp quái chưa có exp ngay
        }

        // Cộng EXP đi dạo
        if (expGain > 0) {
            character.setExp(character.getExp() + expGain.intValue());

            // Level Up Check
            long reqExp = 100L * (long)Math.pow(character.getLv(), 2);
            if (character.getExp() >= reqExp) {
                character.setExp(character.getExp() - (int)reqExp);
                character.setLv(character.getLv() + 1);
                character.setMaxHp(character.getMaxHp() + 20);
                character.setHp(character.getMaxHp());
                character.setBaseAtk(character.getBaseAtk() + 2);
                character.setBaseDef(character.getBaseDef() + 1);

                newLevel = character.getLv();
                message += " [LÊN CẤP!]";
            }
        }

        characterRepository.save(character);

        return new ExplorationResponse(
                message, type, gold,
                character.getExp(), character.getLv(),
                character.getEnergy(), character.getMaxEnergy(),
                newLevel
        );
    }
}