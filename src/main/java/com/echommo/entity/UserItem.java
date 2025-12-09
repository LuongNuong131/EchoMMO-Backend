package com.echommo.entity;

import com.echommo.enums.Rarity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "user_items")
public class UserItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_item_id")
    private Long userItemId;

    // [FIX] Thêm @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    private Integer quantity = 1;
    private Boolean isEquipped = false;
    private Boolean isLocked = false;

    @Enumerated(EnumType.STRING)
    private Rarity rarity = Rarity.COMMON;

    private Integer enhanceLevel = 0;

    private String mainStatType;
    private BigDecimal mainStatValue;

    @Column(columnDefinition = "json")
    private String subStats; // Lưu JSON String

    private LocalDateTime acquiredAt;

    @PrePersist
    public void onCreate() {
        this.acquiredAt = LocalDateTime.now();
    }
}