//package com.echommo.service;
//
//import com.echommo.entity.Character;
//import com.echommo.entity.User;
//import com.echommo.entity.Wallet;
//import com.echommo.enums.SpaPackage;
//import com.echommo.repository.CharacterRepository;
//import com.echommo.repository.UserRepository;
//import com.echommo.repository.WalletRepository;
//import jakarta.persistence.EntityNotFoundException;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.util.HashMap;
//import java.util.Map;
//
//@Service
//@Transactional
//public class SpaService {
//
//    @Autowired
//    private UserRepository userRepo;
//
//    @Autowired
//    private CharacterRepository charRepo;
//
//    @Autowired
//    private WalletRepository walletRepo;
//
//    // Nếu bạn cần gọi logic tạo nhân vật mặc định, hãy inject CharacterService
//    // @Autowired private CharacterService characterService;
//
//    public Map<String, Object> processSpaTreatment(Integer userId, String packageType) {
//        // 1. Lấy thông tin User & Character
//        // (Dùng logic fetch tối ưu tương tự GameService)
//        Character character = charRepo.findByUser_UserIdWithUserAndWallet(userId)
//                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy nhân vật!"));
//
//        Wallet wallet = character.getUser().getWallet();
//
//        // 2. Xác định gói Spa
//        SpaPackage spaPackage;
//        try {
//            spaPackage = SpaPackage.valueOf(packageType.toUpperCase());
//        } catch (IllegalArgumentException | NullPointerException e) {
//            throw new RuntimeException("Gói Spa không hợp lệ: " + packageType);
//        }
//
//        // 3. Kiểm tra tiền & Trừ tiền
//        BigDecimal cost = spaPackage.getCost();
//
//        if ("GOLD".equals(spaPackage.getCurrencyType())) {
//            // Thanh toán bằng Vàng
//            if (wallet.getGold().compareTo(cost) < 0) {
//                throw new RuntimeException("Bạn thiếu tiền! Cần " + cost + " Vàng.");
//            }
//            wallet.setGold(wallet.getGold().subtract(cost));
//        } else {
//            // Thanh toán bằng Kim Cương (Giả sử cost là số nguyên)
//            int diamondCost = cost.intValue();
//            if (wallet.getDiamonds() < diamondCost) {
//                throw new RuntimeException("Bạn thiếu tiền! Cần " + diamondCost + " Kim Cương.");
//            }
//            wallet.setDiamonds(wallet.getDiamonds() - diamondCost);
//        }
//
//        // Lưu ví tiền
//        walletRepo.save(wallet);
//
//        // 4. Tính toán hồi phục
//        // Đảm bảo maxHp > 0
//        int maxHp = character.getMaxHp() > 0 ? character.getMaxHp() : 100;
//        int maxEnergy = character.getMaxEnergy() > 0 ? character.getMaxEnergy() : 50;
//
//        // Tính lượng hồi
//        int healAmount = (int) (maxHp * spaPackage.getRecoveryRate());
//        int energyAmount = (int) (maxEnergy * spaPackage.getRecoveryRate());
//
//        // Cộng dồn vào chỉ số hiện tại (không vượt quá Max)
//        int newHp = Math.min(maxHp, character.getCurrentHp() + healAmount);
//        int newEnergy = Math.min(maxEnergy, character.getCurrentEnergy() + energyAmount);
//
//        character.setCurrentHp(newHp);
//        character.setCurrentEnergy(newEnergy);
//
//        // 5. Lưu Character (Quan trọng: saveAndFlush để update ngay)
//        Character savedChar = charRepo.saveAndFlush(character);
//
//        // 6. Trả về kết quả
//        Map<String, Object> result = new HashMap<>();
//        result.put("success", true);
//        result.put("message", "Thư giãn tại " + spaPackage.getName() + " thành công!");
//        result.put("hpRecovered", healAmount);
//        result.put("energyRecovered", energyAmount);
//        result.put("character", savedChar); // Trả về để Frontend update thanh máu
//        result.put("goldRemaining", wallet.getGold());
//        result.put("diamondsRemaining", wallet.getDiamonds());
//
//        return result;
//    }
//}





package com.echommo.service;

import com.echommo.entity.Character;
import com.echommo.entity.Wallet;
import com.echommo.enums.SpaPackage;
import com.echommo.repository.CharacterRepository;
import com.echommo.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class SpaService {

    private final CharacterRepository charRepo;
    private final WalletRepository walletRepo;

    // [CLEAN CODE] Dùng Constructor Injection thay vì @Autowired trên field
    public SpaService(CharacterRepository charRepo, WalletRepository walletRepo) {
        this.charRepo = charRepo;
        this.walletRepo = walletRepo;
    }

    public Map<String, Object> processSpaTreatment(Integer userId, String packageType) {
        // 1. Lấy thông tin Character + User + Wallet
        // Lưu ý: Hàm này phải tồn tại trong CharacterRepository
        Character character = charRepo.findByUser_UserIdWithUserAndWallet(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân vật!"));

        Wallet wallet = character.getUser().getWallet();

        // 2. Xác định gói Spa từ Enum
        SpaPackage spaPackage;
        try {
            spaPackage = SpaPackage.valueOf(packageType.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new RuntimeException("Gói dịch vụ không tồn tại: " + packageType);
        }

        // 3. Xử lý thanh toán
        BigDecimal cost = spaPackage.getCost();

        if ("GOLD".equals(spaPackage.getCurrencyType())) {
            // --- Trả bằng VÀNG ---
            if (wallet.getGold().compareTo(cost) < 0) {
                throw new RuntimeException("Tiểu nhị: 'Khách quan không đủ Vàng! Cần " + cost + " xu.'");
            }
            wallet.setGold(wallet.getGold().subtract(cost));
        } else {
            // --- Trả bằng KIM CƯƠNG ---
            int diamondCost = cost.intValue();
            if (wallet.getDiamonds() < diamondCost) {
                throw new RuntimeException("Tiểu nhị: 'Khách quan không đủ Kim Cương!'");
            }
            wallet.setDiamonds(wallet.getDiamonds() - diamondCost);
        }

        // Lưu ví tiền
        walletRepo.save(wallet);

        // 4. Tính toán hồi phục
        // Logic này khớp với Entity Character dùng @Data (getCurrentHp)
        int maxHp = character.getMaxHp();
        int maxEnergy = character.getMaxEnergy();

        int healAmount = (int) (maxHp * spaPackage.getRecoveryRate());
        int energyAmount = (int) (maxEnergy * spaPackage.getRecoveryRate());

        int currentHp = character.getCurrentHp();
        int currentEnergy = character.getCurrentEnergy();

        // Cộng dồn và đảm bảo không vượt quá Max
        int newHp = Math.min(maxHp, currentHp + healAmount);
        int newEnergy = Math.min(maxEnergy, currentEnergy + energyAmount);

        character.setCurrentHp(newHp);
        character.setCurrentEnergy(newEnergy);

        // 5. Lưu Character
        charRepo.save(character);

        // 6. Trả kết quả về Controller
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Sử dụng " + spaPackage.getName() + " thành công!");
        result.put("hpRecovered", healAmount);
        result.put("energyRecovered", energyAmount);
        result.put("currentHp", newHp);
        result.put("currentEnergy", newEnergy);
        result.put("goldRemaining", wallet.getGold());
        // Nếu muốn trả về số dư KC: result.put("diamondsRemaining", wallet.getDiamonds());

        return result;
    }
}