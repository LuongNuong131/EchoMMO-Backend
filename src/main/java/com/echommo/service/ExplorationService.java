package com.echommo.service;

import com.echommo.dto.ExplorationResponse;
import com.echommo.entity.Character;
import com.echommo.entity.Wallet;
import com.echommo.repository.CharacterRepository;
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
    @Autowired private CaptchaService captchaService;

    @Transactional
    public ExplorationResponse explore() {
        Character c = characterService.getMyCharacter();
        captchaService.checkLockStatus(c.getUser());

        if (c.getEnergy() < 2) {
            return new ExplorationResponse("Cần nghỉ ngơi...", "REST", BigDecimal.ZERO, c.getExp(), c.getLv(), c.getEnergy(), c.getMaxEnergy(), null);
        }
        c.setEnergy(c.getEnergy() - 2);

        Random r = new Random();
        int roll = r.nextInt(100);
        String type;
        String msg;
        BigDecimal gold = BigDecimal.ZERO;
        Integer newLv = null;

        if (roll < 50) {
            type = "NOTHING";
            msg = "Khu rừng yên tĩnh...";
        } else if (roll < 80) {
            type = "GOLD";
            int g = 10 + r.nextInt(20);
            gold = BigDecimal.valueOf(g);
            msg = "Nhặt được " + g + " Vàng!";
            Wallet w = c.getUser().getWallet();
            w.setGold(w.getGold().add(gold));
            walletRepository.save(w);
        } else {
            type = "ENEMY";
            msg = "Có sát khí!";
        }

        c.setExp(c.getExp() + 5);
        long req = 100L * (long)Math.pow(c.getLv(), 2);
        if(c.getExp() >= req) {
            c.setExp(c.getExp() - (int)req);
            c.setLv(c.getLv() + 1);
            c.setMaxHp(c.getMaxHp() + 20);
            c.setHp(c.getMaxHp());
            newLv = c.getLv();
        }

        characterRepository.save(c);
        return new ExplorationResponse(msg, type, gold, c.getExp(), c.getLv(), c.getEnergy(), c.getMaxEnergy(), newLv);
    }
}