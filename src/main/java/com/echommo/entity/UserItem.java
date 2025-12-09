package com.echommo.entity;

import com.echommo.enums.Rarity;
import com.fasterxml.jackson.annotation.JsonIgnore; // Import quan trọng
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore // [FIX QUAN TRỌNG] Ngăn lỗi "Could not write JSON" và lặp vô tận
    private User user;

    @ManyToOne(fetch = FetchType.EAGER) // Nên để Eager để lấy luôn thông tin item (tên, ảnh) hiển thị
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "is_equipped")
    private Boolean isEquipped = false;

    // --- XỬ LÝ XUNG ĐỘT TÊN GỌI (Enhance vs Enhancement) ---
    @Column(name = "enhancement_level")
    private Integer enhancementLevel = 0;

    // Helper cho các Service cũ gọi getEnhanceLevel
    public Integer getEnhanceLevel() {
        return this.enhancementLevel;
    }

    public void setEnhanceLevel(Integer level) {
        this.enhancementLevel = level;
    }

    @Column(name = "acquired_at")
    private LocalDateTime acquiredAt;

    private Integer quantity;

    @Enumerated(EnumType.STRING)
    private Rarity rarity;

    private String mainStatType;

    private BigDecimal mainStatValue;

    @Column(columnDefinition = "TEXT")
    private String subStats;

    @PrePersist
    protected void onCreate() {
        if (acquiredAt == null) {
            acquiredAt = LocalDateTime.now();
        }
    }
}