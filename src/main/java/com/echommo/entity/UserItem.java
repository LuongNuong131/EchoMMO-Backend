package com.echommo.entity;

import com.echommo.enums.Rarity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_items")
@Data
public class UserItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_item_id")
    private Long userItemId; // DB là BIGINT nên Java là Long

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    private Integer quantity;

    @Column(name = "is_equipped")
    private Boolean isEquipped;

    @Column(name = "is_locked")
    private Boolean isLocked;

    // --- RNG Stats System ---
    @Enumerated(EnumType.STRING)
    private Rarity rarity;

    @Column(name = "enhance_level")
    private Integer enhanceLevel;

    @Column(name = "main_stat_type")
    private String mainStatType; // VD: ATK_FLAT, HP_PERCENT

    @Column(name = "main_stat_value")
    private BigDecimal mainStatValue;

    // Lưu chuỗi JSON thô để an toàn nhất cho DB, Service sẽ xử lý parse sau
    // columnDefinition = "json" giúp MySQL hiểu, nhưng Java vẫn coi là String
    @Column(name = "sub_stats", columnDefinition = "json")
    private String subStats;

    @Column(name = "acquired_at")
    private LocalDateTime acquiredAt;
}