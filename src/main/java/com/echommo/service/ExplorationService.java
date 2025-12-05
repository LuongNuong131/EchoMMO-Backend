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
    @Autowired private QuestService questService;
    @Autowired private UserRepository userRepository;
    @Autowired private CaptchaService captchaService;

    private final Random random = new Random();

    @Transactional
    public ExplorationResponse explore() {
        Character character = characterService.getMyCharacter();
        User user = character.getUser();

        captchaService.checkLockStatus(user);

        if (Boolean.TRUE.equals(user.getIsCaptchaLocked())) {
            throw new RuntimeException("CAPTCHA_REQUIRED");
        }

        // SỬA TỶ LỆ CAPTCHA (Fix #12): Giảm xuống 1% (Tương đương 1/100 lần)
        if (random.nextInt(100) < 1) {
            user.setIsCaptchaLocked(true);
            userRepository.save(user);
            throw new RuntimeException("CAPTCHA_REQUIRED");
        }

        if (character.getEnergy() < 1) {
            throw new RuntimeException("Hết năng lượng rồi! Hãy nghỉ ngơi.");
        }

        character.setEnergy(character.getEnergy() - 1);

        int roll = random.nextInt(100);
        String message;
        String type;
        BigDecimal gold = BigDecimal.ZERO;
        Long exp = 0L;
        Integer newLevel = null;

        Wallet wallet = user.getWallet();

        if (roll < 25) {
            int amount = 10 + random.nextInt(41);
            gold = new BigDecimal(amount);
            wallet.setGold(wallet.getGold().add(gold));
            message = "💰 Bạn tìm thấy " + amount + " Gold!";
            type = "GOLD";
        } else if (roll < 75) {
            exp = 15L + random.nextInt(16);
            boolean isLevelUp = addExp(character, exp);
            message = "⚔️ Tiêu diệt quái vật, nhận " + exp + " EXP!";
            if (isLevelUp) {
                message += " 🆙 LÊN CẤP " + character.getLevel() + "!";
                newLevel = character.getLevel();
                type = "LEVEL_UP";
            } else {
                type = "EXP";
            }
        } else {
            if (random.nextBoolean()) {
                int qty = 1 + random.nextInt(3);
                wallet.setWood(wallet.getWood() + qty);
                message = "🌲 Bạn chặt được " + qty + " khúc Gỗ.";
                type = "MATERIAL";
            } else {
                int qty = 1 + random.nextInt(3);
                wallet.setStone(wallet.getStone() + qty);
                message = "🪨 Bạn đào được " + qty + " viên Đá.";
                type = "MATERIAL";
            }
        }

        questService.updateProgress(user, "EXPLORE", 1);
        characterRepository.save(character);
        walletRepository.save(wallet);

        return new ExplorationResponse(message, type, gold, exp, character.getEnergy(), character.getMaxEnergy(), newLevel);
    }

    private boolean addExp(Character character, Long amount) {
        character.setCurrentExp(character.getCurrentExp() + amount);
        long expNeeded = character.getLevel() * 100L;

        if (character.getCurrentExp() >= expNeeded) {
            character.setLevel(character.getLevel() + 1);
            character.setCurrentExp(character.getCurrentExp() - expNeeded);
            character.setMaxHp(character.getMaxHp() + 20);
            character.setHp(character.getMaxHp());
            character.setAtk(character.getAtk() + 2);
            character.setDef(character.getDef() + 1);
            character.setEnergy(character.getMaxEnergy());
            return true;
        }
        return false;
    }
}