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

    public List<DailyQuest> getDailyQuests() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        List<DailyQuest> todayQuests = questRepository.findByUser_UserIdAndCreatedDate(user.getUserId(), LocalDate.now());

        if (todayQuests.isEmpty()) {
            generateNewQuests(user);
            return questRepository.findByUser_UserIdAndCreatedDate(user.getUserId(), LocalDate.now());
        }
        return todayQuests;
    }

    private void generateNewQuests(User user) {
        createQuest(user, "EXPLORE", "Đi khám phá 10 bước", 10, 50, 20);
        createQuest(user, "KILL_ENEMY", "Tiêu diệt 3 quái vật", 3, 100, 50);
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
        q.setProgress(0);
        q.setIsClaimed(false);
        q.setCreatedDate(LocalDate.now());
        questRepository.save(q);
    }

    @Transactional
    public void updateProgress(User user, String type, int amount) {
        List<DailyQuest> quests = questRepository.findByUser_UserIdAndCreatedDate(user.getUserId(), LocalDate.now());
        for (DailyQuest q : quests) {
            if (q.getType().equals(type) && !Boolean.TRUE.equals(q.getIsClaimed()) && q.getProgress() < q.getTarget()) {
                q.setProgress(Math.min(q.getTarget(), q.getProgress() + amount));
                questRepository.save(q);
            }
        }
    }

    @Transactional
    public String claimReward(Integer questId) {
        DailyQuest q = questRepository.findById(questId)
                .orElseThrow(() -> new RuntimeException("Quest not found"));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!q.getUser().getUsername().equals(username)) throw new RuntimeException("Không phải quest của bạn");

        if (!q.isCompleted()) return "Chưa hoàn thành!";
        if (Boolean.TRUE.equals(q.getIsClaimed())) return "Đã nhận rồi!";

        q.setIsClaimed(true);
        questRepository.save(q);

        Wallet w = q.getUser().getWallet();

        // [FIX] Convert reward sang BigDecimal và cộng
        long reward = q.getRewardAmount() != null ? q.getRewardAmount() : 0;
        w.setGold(w.getGold().add(BigDecimal.valueOf(reward)));

        walletRepository.save(w);

        return "Nhận thành công " + reward + " " + q.getRewardType() + "!";
    }
}