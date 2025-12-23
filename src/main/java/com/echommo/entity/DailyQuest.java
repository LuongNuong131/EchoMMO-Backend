package com.echommo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "daily_quests")
public class DailyQuest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    private String type; // EXPLORE, KILL_ENEMY
    private String description;
    private Integer target;
    private Integer progress = 0;

    @Column(name = "reward_gold")
    private Integer rewardGold;

    @Column(name = "reward_exp")
    private Integer rewardExp;

    @Column(name = "is_claimed")
    private Boolean isClaimed = false;

    @Column(name = "created_date")
    private LocalDate createdDate = LocalDate.now();

    // --- [FIX] CÁC HÀM HELPER "MA" ĐÃ ĐƯỢC HỒI SINH ---
    public boolean isCompleted() {
        return this.progress >= this.target;
    }

    public void setCompleted(boolean completed) {
        if (completed) {
            this.progress = this.target;
        }
    }

    public String getRewardType() {
        // Mặc định quest thường thưởng Vàng
        return "GOLD";
    }

    public Integer getRewardAmount() {
        return this.rewardGold != null ? this.rewardGold : 0;
    }
}