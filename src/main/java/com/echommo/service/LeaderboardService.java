package com.echommo.service;

import com.echommo.dto.LeaderboardEntry; // Đảm bảo đã có file này như hướng dẫn trước
import com.echommo.entity.Character;
import com.echommo.repository.CharacterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaderboardService {

    @Autowired
    private CharacterRepository charRepo;

    // 1. Hàm lấy BXH Level (Sửa tên từ getTopPlayers -> getLevelLeaderboard)
    public List<LeaderboardEntry> getLevelLeaderboard() {
        // Lấy list nhân vật, sắp xếp theo Level giảm dần, nếu bằng Level thì so EXP
        List<Character> characters = charRepo.findAll(Sort.by(Sort.Direction.DESC, "level", "currentExp"));

        // Chỉ lấy Top 10 và chuyển sang DTO LeaderboardEntry
        return characters.stream()
                .limit(10)
                .map(c -> {
                    String name = (c.getName() != null) ? c.getName() : "Unknown";
                    // Nếu chưa có tên Character thì lấy tên User (nếu có quan hệ)
                    if (c.getName() == null && c.getUser() != null) {
                        name = c.getUser().getUsername();
                    }

                    // Trả về DTO: name, value (level)
                    return new LeaderboardEntry(name, c.getLevel());
                })
                .collect(Collectors.toList());
    }

    // 2. Hàm lấy BXH Tài sản (Thêm mới để Controller không báo lỗi)
    public List<LeaderboardEntry> getWealthLeaderboard() {
        // Giả sử field tiền là "gold", sắp xếp giảm dần
        // Lưu ý: Nếu trong entity Character chưa có "gold", hãy thêm field đó vào hoặc đổi tên field cho đúng
        List<Character> characters = charRepo.findAll(Sort.by(Sort.Direction.DESC, "gold"));

        return characters.stream()
                .limit(10)
                .map(c -> {
                    String name = (c.getName() != null) ? c.getName() : "Unknown";
                    if (c.getName() == null && c.getUser() != null) {
                        name = c.getUser().getUsername();
                    }
                    // Trả về DTO: name, value (gold)
                    return new LeaderboardEntry(name, c.getGold());
                })
                .collect(Collectors.toList());
    }
}