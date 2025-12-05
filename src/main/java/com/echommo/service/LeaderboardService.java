package com.echommo.service;

import com.echommo.dto.LeaderboardEntry;
import com.echommo.entity.Character;
import com.echommo.entity.Wallet;
import com.echommo.repository.CharacterRepository;
import com.echommo.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LeaderboardService {

    @Autowired
    private CharacterRepository characterRepository;

    @Autowired
    private WalletRepository walletRepository;

    // BXH Cấp Độ
    public List<LeaderboardEntry> getLevelLeaderboard() {
        // Giả định phương thức này đã được khai báo trong CharacterRepository
        List<Character> chars = characterRepository.findTop10ByOrderByLvDescExpDesc();
        List<LeaderboardEntry> result = new ArrayList<>();

        for (int i = 0; i < chars.size(); i++) {
            Character c = chars.get(i);
            // FIX: Sử dụng c.getLv() thay vì getLevel()
            String name = c.getName() != null ? c.getName() : c.getUser().getUsername();

            result.add(new LeaderboardEntry(
                    name,
                    "Cấp " + c.getLv(),
                    "#" + (i + 1),
                    name.substring(0, 1).toUpperCase()
            ));
        }
        return result;
    }

    // BXH Đại Gia (Vàng)
    public List<LeaderboardEntry> getWealthLeaderboard() {
        // Giả định phương thức này đã được khai báo trong WalletRepository
        List<Wallet> wallets = walletRepository.findTop10ByOrderByGoldDesc();
        List<LeaderboardEntry> result = new ArrayList<>();

        for (int i = 0; i < wallets.size(); i++) {
            Wallet w = wallets.get(i);

            // Lấy tên từ User
            String name = w.getUser().getUsername();

            // Sử dụng longValue() để đảm bảo chuyển đổi từ BigDecimal
            String goldString = w.getGold().longValue() + " 🟡";

            result.add(new LeaderboardEntry(
                    name,
                    goldString,
                    "#" + (i + 1),
                    name.substring(0, 1).toUpperCase()
            ));
        }
        return result;
    }
}