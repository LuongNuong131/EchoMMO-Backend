package com.echommo.service;

import com.echommo.dto.LeaderboardEntry;
import com.echommo.entity.Character;
import com.echommo.entity.Wallet;
import com.echommo.repository.CharacterRepository;
import com.echommo.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class LeaderboardService {

    @Autowired
    private CharacterRepository charRepo;

    @Autowired
    private WalletRepository walletRepo;

    // 1. BXH Level
    public List<LeaderboardEntry> getLevelLeaderboard() {
        // Lấy Top 10 theo Level và Exp
        List<Character> characters = charRepo.findTop10ByOrderByLevelDescCurrentExpDesc();

        // Biến đếm hạng (1, 2, 3...)
        AtomicInteger rankCounter = new AtomicInteger(1);

        return characters.stream()
                .map(c -> {
                    // Lấy tên
                    String name = (c.getName() != null) ? c.getName() : "Unknown";
                    if (c.getName() == null && c.getUser() != null) {
                        name = c.getUser().getUsername();
                    }

                    // [FIX] Tạo đủ 4 trường String cho DTO
                    String valueDisplay = "Lv " + c.getLevel();
                    String rankDisplay = String.valueOf(rankCounter.getAndIncrement());
                    String avatarDisplay = (name.length() > 0) ? String.valueOf(name.charAt(0)).toUpperCase() : "?";

                    return new LeaderboardEntry(name, valueDisplay, rankDisplay, avatarDisplay);
                })
                .collect(Collectors.toList());
    }

    // 2. BXH Tài sản (Lấy từ Wallet)
    public List<LeaderboardEntry> getWealthLeaderboard() {
        // [FIX] Lấy từ WalletRepository thay vì CharacterRepository
        List<Wallet> wallets = walletRepo.findTop10ByOrderByGoldDesc();

        AtomicInteger rankCounter = new AtomicInteger(1);

        return wallets.stream()
                .map(w -> {
                    String name = "Unknown";
                    if (w.getUser() != null) {
                        name = w.getUser().getUsername(); // Lấy tên username của chủ ví
                    }

                    // [FIX] Tạo đủ 4 trường String cho DTO
                    // Giả sử gold là BigDecimal, format bỏ số thập phân thừa
                    String valueDisplay = String.format("%.0f Vàng", w.getGold());

                    String rankDisplay = String.valueOf(rankCounter.getAndIncrement());
                    String avatarDisplay = (name.length() > 0) ? String.valueOf(name.charAt(0)).toUpperCase() : "?";

                    return new LeaderboardEntry(name, valueDisplay, rankDisplay, avatarDisplay);
                })
                .collect(Collectors.toList());
    }
}