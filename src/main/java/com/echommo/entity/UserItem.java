package com.echommo.entity;

import com.echommo.enums.Rarity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY) // [FIX 1] Quan trọng: Khớp với AUTO_INCREMENT trong SQL
    @Column(name = "user_item_id")
    private Long userItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "char_id", nullable = false) // [FIX 2] Quan trọng: Chặn lỗi null char_id
    @JsonIgnore
    private Character character;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Builder.Default
    @Column(name = "is_equipped")
    private Boolean isEquipped = false;

    @Builder.Default
    @Column(name = "is_locked")
    private Boolean isLocked = false; // Giữ lại field này cho InventoryService

    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "acquired_at")
    private LocalDateTime acquiredAt;

    @Builder.Default
    @Column(name = "enhance_level")
    private Integer enhanceLevel = 0; // Giữ nguyên tên này, đừng đổi thành upgradeLevel

    @Enumerated(EnumType.STRING)
    private Rarity rarity;

    @Column(name = "main_stat_type")
    private String mainStatType;

    @Column(name = "main_stat_value")
    private BigDecimal mainStatValue;

    @Column(name = "sub_stats", columnDefinition = "TEXT")
    private String subStats;

    @Builder.Default
    @Column(name = "visual_variant")
    private Integer visualVariant = 0;

    @Builder.Default
    @Column(name = "is_mythic")
    private boolean isMythic = false;

    @Builder.Default
    @Column(name = "mythic_stars")
    private Integer mythicStars = 0;

    // Giữ nguyên logic tách Max/Current Durability như code cũ
    @Column(name = "current_durability")
    @Builder.Default
    private Integer currentDurability = 100;

    @Column(name = "max_durability")
    @Builder.Default
    private Integer maxDurability = 100;

    @Column(name = "original_main_stat_value")
    private BigDecimal originalMainStatValue;

    @PrePersist
    protected void onCreate() { if (acquiredAt == null) acquiredAt = LocalDateTime.now(); }

    @JsonIgnore
    public User getUser() {
        return this.character != null ? this.character.getUser() : null;
    }

    // Các helper method giữ nguyên để tương thích code cũ
    public Integer getLevel() { return this.enhanceLevel != null ? this.enhanceLevel : 0; }
    public void setLevel(Integer level) { this.enhanceLevel = level; }
    public Boolean getIsMythic() { return isMythic; }
    public void setIsMythic(boolean v) { this.isMythic = v; }
    public Boolean getIsLocked() { return isLocked != null ? isLocked : false; }
}