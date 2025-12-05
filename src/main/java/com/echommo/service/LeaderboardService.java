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
        List<Character> chars = characterRepository.findTop10ByOrderByLevelDescCurrentExpDesc();
        List<LeaderboardEntry> result = new ArrayList<>();

        for (int i = 0; i < chars.size(); i++) {
            Character c = chars.get(i);
            result.add(new LeaderboardEntry(
                    c.getName(),
                    "Lv. " + c.getLevel(),
                    "#" + (i + 1),
                    c.getName().substring(0, 1).toUpperCase()
            ));
        }
        return result;
    }

    // BXH Đại Gia (Vàng)
    public List<LeaderboardEntry> getWealthLeaderboard() {
        List<Wallet> wallets = walletRepository.findTop10ByOrderByGoldDesc();
        List<LeaderboardEntry> result = new ArrayList<>();

        for (int i = 0; i < wallets.size(); i++) {
            Wallet w = wallets.get(i);
            // Lấy tên từ User, nếu chưa tạo Char thì lấy Username
            String name = w.getUser().getUsername();

            // Logic tìm tên nhân vật (hơi thủ công chút do quan hệ DB)
            // Ở đây t dùng tạm Username cho nhanh, vì Wallet đi theo User

            result.add(new LeaderboardEntry(
                    name,
                    w.getGold().intValue() + " 🟡",
                    "#" + (i + 1),
                    name.substring(0, 1).toUpperCase()
            ));
        }
        return result;
    }
}