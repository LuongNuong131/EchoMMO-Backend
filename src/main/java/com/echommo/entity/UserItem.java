package com.echommo.entity;

import com.echommo.enums.Rarity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userItemId;

    // --- RELATIONSHIPS ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore // [CHUẨN] Ngăn lặp vô tận khi convert JSON
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    // --- BASIC STATS ---
    @Column(name = "is_equipped")
    private Boolean isEquipped = false;

    private Integer quantity;

    @Column(name = "acquired_at")
    private LocalDateTime acquiredAt;

    // --- [FIX QUAN TRỌNG] UPGRADE SYSTEM ---

    // Trong SQL tên cột là "enhance_level", trong Java bro muốn dùng "enhancementLevel"
    // => @Column phải map đúng tên trong SQL
    @Column(name = "enhance_level")
    private Integer enhancementLevel = 0;

    // Helper method: Giữ lại để tương thích với Service cũ dùng .getEnhanceLevel()
    public Integer getEnhanceLevel() {
        return this.enhancementLevel;
    }

    public void setEnhanceLevel(Integer level) {
        this.enhancementLevel = level;
    }

    // --- STATS SYSTEM ---
    @Enumerated(EnumType.STRING)
    private Rarity rarity;

    private String mainStatType;

    private BigDecimal mainStatValue;

    // [QUAN TRỌNG] Lưu Substats dưới dạng JSON String
    // columnDefinition = "json" giúp MySQL hiểu đây là JSON (nếu dùng MySQL 5.7+)
    // Nếu lỗi, đổi thành "TEXT" cũng được.
    @Column(name = "sub_stats", columnDefinition = "json")
    private String subStats;

    // --- [NEW] MYTHIC / RED EVOLUTION FIELDS ---
    // Các trường này BẮT BUỘC CÓ để chạy logic tiến hóa 5%

    @Column(name = "is_mythic")
    private boolean isMythic = false;

    @Column(name = "mythic_level")
    private Integer mythicLevel = 0;

    // [SNAPSHOT] Lưu chỉ số Main gốc để nhân 1% mỗi cấp
    @Column(name = "original_main_stat_value")
    private BigDecimal originalMainStatValue;

    // [OPTIONAL] Điểm may mắn (nếu sau này bro đổi ý muốn dùng bảo hiểm)
    @Column(name = "luck_points")
    private Integer luckPoints = 0;

    @PrePersist
    protected void onCreate() {
        if (acquiredAt == null) {
            acquiredAt = LocalDateTime.now();
        }
    }
}