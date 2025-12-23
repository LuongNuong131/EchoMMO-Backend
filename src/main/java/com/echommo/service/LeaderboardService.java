package com.echommo.service;

import com.echommo.dto.LeaderboardEntry;
import com.echommo.entity.User;
import com.echommo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class LeaderboardService {

    @Autowired
    private UserRepository userRepository;

    // Helper: Chọn avatar ưu tiên (Ảnh upload -> Skin game)
    private String getDisplayAvatar(User user) {
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            return user.getProfileImageUrl();
        }
        return user.getAvatarUrl();
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getTopLevels() {
        Pageable top10 = PageRequest.of(0, 10);
        List<User> users = userRepository.findTop10ByOrderByCharacter_LevelDesc(top10);

        return mapUsersToEntries(users, "LEVEL");
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getTopWealth() {
        Pageable top10 = PageRequest.of(0, 10);
        List<User> users = userRepository.findTop10ByOrderByWallet_GoldDesc(top10);

        return mapUsersToEntries(users, "WEALTH");
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getTopMonsters() {
        Pageable top10 = PageRequest.of(0, 10);
        // Lưu ý: Đảm bảo tên hàm trong UserRepository khớp với field monsterKills
        List<User> users = userRepository.findTop10ByOrderByCharacter_MonsterKillsDesc(top10);

        return mapUsersToEntries(users, "MONSTER");
    }

    // Hàm chuyển đổi chung để tránh lặp code
    private List<LeaderboardEntry> mapUsersToEntries(List<User> users, String type) {
        List<LeaderboardEntry> result = new ArrayList<>();

        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            String username = user.getUsername();
            String avatar = getDisplayAvatar(user);
            String rank = String.valueOf(i + 1); // Rank bắt đầu từ 1
            String value = "";

            switch (type) {
                case "LEVEL":
                    int level = (user.getCharacter() != null) ? user.getCharacter().getLevel() : 0;
                    value = "Lv. " + level;
                    break;
                case "WEALTH":
                    // Format số tiền (ví dụ: 1,000,000 Gold)
                    String gold = (user.getWallet() != null) ? String.format("%,.0f", user.getWallet().getGold()) : "0";
                    value = gold + " Gold";
                    break;
                case "MONSTER":
                    // [FIX] Gọi đúng getter getMonsterKills()
                    int kills = (user.getCharacter() != null) ? user.getCharacter().getMonsterKills() : 0;
                    value = String.format("%,d Trảm", kills);
                    break;
            }

            // Gọi đúng Constructor 4 tham số: username, value, rank, avatar
            result.add(new LeaderboardEntry(username, value, rank, avatar));
        }

        return result;
    }
}