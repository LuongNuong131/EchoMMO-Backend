package com.echommo.entity;

import com.echommo.enums.Rarity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime; // Import mới cho lỗi setAcquiredAt

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
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "is_equipped")
    private Boolean isEquipped = false;

    // --- XỬ LÝ XUNG ĐỘT TÊN GỌI (Enhance vs Enhancement) ---

    // Chúng ta chọn 'enhancementLevel' làm tên chính trong Database
    @Column(name = "enhancement_level")
    private Integer enhancementLevel = 0;
    // Lombok sẽ tự sinh: getEnhancementLevel(), setEnhancementLevel() -> Fix InventoryServiceImpl

    // Thêm thủ công 2 hàm này để chiều lòng MarketplaceService (fix lỗi cũ)
    public Integer getEnhanceLevel() {
        return this.enhancementLevel;
    }

    public void setEnhanceLevel(Integer level) {
        this.enhancementLevel = level;
    }

    // --- CÁC FIELD MỚI THÊM ---

    // Fix lỗi: setAcquiredAt(LocalDateTime)
    @Column(name = "acquired_at")
    private LocalDateTime acquiredAt;

    // --- CÁC FIELD CŨ ---

    private Integer quantity;

    @Enumerated(EnumType.STRING)
    private Rarity rarity;

    private String mainStatType;

    private BigDecimal mainStatValue;

    @Column(columnDefinition = "TEXT")
    private String subStats;

    // Hàm tiện ích: Tự động set thời gian khi tạo mới (Optional)
    @PrePersist
    protected void onCreate() {
        if (acquiredAt == null) {
            acquiredAt = LocalDateTime.now();
        }
    }
}