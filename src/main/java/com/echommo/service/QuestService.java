package com.echommo.service;

import com.echommo.entity.DailyQuest;
import com.echommo.entity.User;
import com.echommo.entity.Wallet;
import com.echommo.repository.DailyQuestRepository;
import com.echommo.repository.UserRepository;
import com.echommo.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@Service
public class QuestService {
    @Autowired private DailyQuestRepository questRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private WalletRepository walletRepository;

    private final Random random = new Random();

    // 1. Lấy hoặc Tạo danh sách Quest hôm nay
    public List<DailyQuest> getDailyQuests() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        List<DailyQuest> todayQuests = questRepository.findByUser_UserIdAndCreatedDate(user.getUserId(), LocalDate.now());

        if (todayQuests.isEmpty()) {
            // Tạo 3 nhiệm vụ ngẫu nhiên
            generateNewQuests(user);
            return questRepository.findByUser_UserIdAndCreatedDate(user.getUserId(), LocalDate.now());
        }
        return todayQuests;
    }

    private void generateNewQuests(User user) {
        // Quest 1: Đi bộ
        createQuest(user, "EXPLORE", "Đi khám phá 10 bước", 10, 50, 20);
        // Quest 2: Diệt quái
        createQuest(user, "KILL_ENEMY", "Tiêu diệt 3 quái vật", 3, 100, 50);
        // Quest 3: Random (Đi bộ nhiều hơn)
        if (random.nextBoolean()) {
            createQuest(user, "EXPLORE", "Đi khám phá 30 bước", 30, 150, 60);
        } else {
            createQuest(user, "KILL_ENEMY", "Tiêu diệt 10 quái vật", 10, 300, 150);
        }
    }

    private void createQuest(User user, String type, String desc, int target, int gold, int exp) {
        DailyQuest q = new DailyQuest();
        q.setUser(user);
        q.setType(type);
        q.setDescription(desc);
        q.setTarget(target);
        q.setRewardGold(gold);
        q.setRewardExp(exp);
        questRepository.save(q);
    }

    // 2. Cập nhật tiến độ (Gọi từ Battle/Exploration Service)
    @Transactional
    public void updateProgress(User user, String type, int amount) {
        List<DailyQuest> quests = questRepository.findByUser_UserIdAndCreatedDate(user.getUserId(), LocalDate.now());
        for (DailyQuest q : quests) {
            if (q.getType().equals(type) && !q.getIsClaimed() && q.getProgress() < q.getTarget()) {
                q.setProgress(Math.min(q.getTarget(), q.getProgress() + amount));
                questRepository.save(q);
            }
        }
    }

    // 3. Nhận thưởng
    @Transactional
    public String claimReward(Integer questId) {
        DailyQuest q = questRepository.findById(questId).orElseThrow();

        // Check quyền sở hữu (đơn giản)
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!q.getUser().getUsername().equals(username)) throw new RuntimeException("Không phải quest của bạn");

        if (q.getIsClaimed()) return "Đã nhận rồi!";
        if (q.getProgress() < q.getTarget()) return "Chưa hoàn thành!";

        q.setIsClaimed(true);
        questRepository.save(q);

        Wallet w = q.getUser().getWallet();
        w.setGold(w.getGold().add(new BigDecimal(q.getRewardGold())));
        walletRepository.save(w);

        return "Nhận thành công " + q.getRewardGold() + " Vàng!";
    }
}