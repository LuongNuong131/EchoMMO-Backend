//package com.echommo.service;
//
//import com.echommo.dto.ExplorationResponse;
//import com.echommo.entity.Character;
//import com.echommo.entity.User;
//import com.echommo.entity.Wallet;
//import com.echommo.repository.CharacterRepository;
//import com.echommo.repository.UserRepository;
//import com.echommo.repository.WalletRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import java.math.BigDecimal;
//import java.util.Random;
//
//@Service
//public class ExplorationService {
//    @Autowired private CharacterService characterService;
//    @Autowired private CharacterRepository characterRepository;
//    @Autowired private WalletRepository walletRepository;
//    @Autowired private UserRepository userRepository;
//    @Autowired private CaptchaService captchaService;
//
//    @Transactional
//    public ExplorationResponse explore() {
//        Character c = characterService.getMyCharacter();
//
//        // [FIX] Tự động tạo nhân vật nếu chưa có (cho trường hợp Admin)
//        if (c == null) {
//            String username = SecurityContextHolder.getContext().getAuthentication().getName();
//            User user = userRepository.findByUsername(username).orElseThrow();
//            c = characterService.createDefaultCharacter(user);
//        }
//
//        captchaService.checkLockStatus(c.getUser());
//
//        // Logic Energy GameFi (Cost = 2)
//        if (c.getEnergy() < 2) {
//            return new ExplorationResponse("Cần nghỉ ngơi... (Hết thể lực)", "REST", BigDecimal.ZERO, c.getExp(), c.getLv(), c.getEnergy(), c.getMaxEnergy(), null);
//        }
//        c.setEnergy(c.getEnergy() - 2);
//
//        Random r = new Random();
//        int roll = r.nextInt(100);
//        String type;
//        String msg;
//        BigDecimal gold = BigDecimal.ZERO;
//        Integer newLv = null;
//
//        if (roll < 50) {
//            type = "NOTHING";
//            msg = "Khu rừng yên tĩnh...";
//        } else if (roll < 80) {
//            type = "GOLD";
//            int g = 10 + r.nextInt(20);
//            gold = BigDecimal.valueOf(g);
//            msg = "Nhặt được " + g + " Vàng!";
//            Wallet w = c.getUser().getWallet();
//            w.setGold(w.getGold().add(gold));
//            walletRepository.save(w);
//        } else {
//            type = "ENEMY";
//            msg = "Có sát khí! Chuẩn bị chiến đấu!";
//        }
//
//        // Cộng EXP nhẹ khi đi thám hiểm
//        c.setExp(c.getExp() + 5);
//        long req = 100L * (long)Math.pow(c.getLv(), 2);
//        if(c.getExp() >= req) {
//            c.setExp(c.getExp() - (int)req);
//            c.setLv(c.getLv() + 1);
//            c.setMaxHp(c.getMaxHp() + 20);
//            c.setHp(c.getMaxHp());
//            newLv = c.getLv();
//            msg += " [LÊN CẤP!]";
//        }
//
//        characterRepository.save(c);
//        return new ExplorationResponse(msg, type, gold, c.getExp(), c.getLv(), c.getEnergy(), c.getMaxEnergy(), newLv);
//    }
//}

package com.echommo.service;

import com.echommo.dto.ExplorationResponse;
import com.echommo.entity.Character;
import com.echommo.entity.User;
import com.echommo.entity.Wallet;
import com.echommo.repository.CharacterRepository;
import com.echommo.repository.UserRepository;
import com.echommo.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Transactional
    public ExplorationResponse explore() {
        try {
            Character c = characterService.getMyCharacter();

            // [AUTO-CREATE] Nếu chưa có nhân vật, tạo ngay lập tức
            if (c == null) {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("User not found in DB"));
                c = characterService.createDefaultCharacter(user);
            }

            if (c == null) throw new RuntimeException("Không thể tạo nhân vật. Hãy thử đăng nhập lại.");

            captchaService.checkLockStatus(c.getUser());

            if (c.getEnergy() < 2) {
                return new ExplorationResponse("Đại hiệp mệt rồi, cần nghỉ ngơi (Hết thể lực)...", "REST", BigDecimal.ZERO, c.getExp(), c.getLv(), c.getEnergy(), c.getMaxEnergy(), null);
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
                msg = "Khu rừng yên tĩnh lạ thường...";
            } else if (roll < 80) {
                type = "GOLD";
                int g = 10 + r.nextInt(20);
                gold = BigDecimal.valueOf(g);
                msg = "Nhặt được " + g + " Vàng rơi bên đường!";
                Wallet w = c.getUser().getWallet();
                w.setGold(w.getGold().add(gold));
                walletRepository.save(w);
            } else {
                type = "ENEMY";
                msg = "Có sát khí! Quái vật xuất hiện!";
            }

            c.setExp(c.getExp() + 5);
            long req = 100L * (long)Math.pow(c.getLv(), 2);
            if(c.getExp() >= req) {
                c.setExp(c.getExp() - (int)req);
                c.setLv(c.getLv() + 1);
                c.setMaxHp(c.getMaxHp() + 20);
                c.setHp(c.getMaxHp());
                c.setEnergy(c.getMaxEnergy()); // Lên cấp hồi full năng lượng
                newLv = c.getLv();
                msg += " [CHÚC MỪNG! ĐÃ LÊN CẤP " + newLv + "]";
            }

            characterRepository.save(c);
            return new ExplorationResponse(msg, type, gold, c.getExp(), c.getLv(), c.getEnergy(), c.getMaxEnergy(), newLv);

        } catch (Exception e) {
            e.printStackTrace(); // In lỗi ra console server để debug
            throw new RuntimeException(e.getMessage());
        }
    }
}