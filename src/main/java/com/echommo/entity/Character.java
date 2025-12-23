package com.echommo.entity;

import com.echommo.enums.CharacterStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "characters", indexes = {
        @Index(name = "idx_char_name", columnList = "name"),
        @Index(name = "idx_user_id", columnList = "user_id")
})
public class Character {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "char_id")
    private Integer charId;

    // --- OPTIMISTIC LOCKING ---
    // Cái này cực quan trọng để tránh bug "hack" đồ hoặc sai lệch chỉ số khi mạng lag
    @Version
    private Long version;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "avatar_url", length = 500)
    @Builder.Default
    private String avatarUrl = "https://i.imgur.com/7Y7t5Xp.png";

    @Builder.Default private Integer level = 1;
    @Builder.Default private Long currentExp = 0L;

    @Column(name = "character_class")
    @Builder.Default private String characterClass = "Nhà Thám Hiểm";

    @Enumerated(EnumType.STRING)
    @Builder.Default private CharacterStatus status = CharacterStatus.IDLE;

    // --- MAIN STATS (Tiềm năng) ---
    @Builder.Default private Integer statPoints = 0;
    @Builder.Default private Integer str = 5;
    @Builder.Default private Integer vit = 5;
    @Builder.Default private Integer agi = 5;
    @Builder.Default private Integer dex = 5;
    @Builder.Default private Integer intelligence = 5;
    @Builder.Default private Integer luck = 5;

    // --- COMPUTED STATS (Chỉ số chiến đấu) ---
    @Builder.Default private Integer currentHp = 200;
    @Builder.Default private Integer maxHp = 200;
    @Builder.Default private Integer currentEnergy = 100;
    @Builder.Default private Integer maxEnergy = 100;
    @Builder.Default private Integer baseAtk = 10;
    @Builder.Default private Integer baseDef = 5;
    @Builder.Default private Integer baseSpeed = 10;
    @Builder.Default private Integer baseCritRate = 50;
    @Builder.Default private Integer baseCritDmg = 150;

    // --- PVP & DANH VỌNG SYSTEM ---
    @Column(name = "total_power")
    @Builder.Default private Integer totalPower = 0;

    // Đổi pvpPoints thành reputation (Danh Vọng)
    @Column(name = "reputation")
    @Builder.Default private Integer reputation = 0;

    @Column(name = "rank_title")
    @Builder.Default private String rankTitle = "Vô Danh";

    // Hai biến quan trọng để tính logic cộng trừ điểm ông yêu cầu
    @Column(name = "win_streak")
    @Builder.Default private Integer winStreak = 0;

    @Column(name = "lose_streak")
    @Builder.Default private Integer loseStreak = 0;

    @Column(name = "pvp_wins")
    @Builder.Default private Integer pvpWins = 0;

    @Column(name = "pvp_matches_played")
    @Builder.Default private Integer pvpMatchesPlayed = 0;

    // --- MAP ---
    @Builder.Default
    @Column(name = "current_map_id")
    private String currentMapId = "MAP_01";

    @Builder.Default private String currentLocation = "Làng Tân Thủ";
    @Builder.Default @Column(name = "monster_kills") private Integer monsterKills = 0;

    // --- SPA SYSTEM ---
    @Column(name = "spa_start_time") private LocalDateTime spaStartTime;
    @Column(name = "spa_end_time") private LocalDateTime spaEndTime;
    @Column(name = "spa_package_type") private String spaPackageType;

    @Column(name = "last_free_spa_use")
    private LocalDateTime lastFreeSpaUse;

    @Column(name = "daily_spa_usage")
    @Builder.Default
    private Integer dailySpaUsage = 0;

    // --- GATHERING SYSTEM ---
    @Column(name = "gathering_level")
    @Builder.Default
    private Integer gatheringLevel = 1;

    @Column(name = "gathering_exp")
    @Builder.Default
    private Long gatheringExp = 0L;

    @Column(name = "gathering_item_id") private Integer gatheringItemId;
    @Column(name = "gathering_remaining_amount") @Builder.Default private Integer gatheringRemainingAmount = 0;
    @Column(name = "gathering_expiry") private LocalDateTime gatheringExpiry;

    // --- TIME ---
    @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime lastActive;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        lastActive = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastActive = LocalDateTime.now();
    }
}