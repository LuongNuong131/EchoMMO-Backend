package com.echommo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // [QUAN TRỌNG] Thêm dòng này
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "market_listings")
public class MarketListing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "listing_id")
    private Integer listingId;

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    // [FIX QUAN TRỌNG] Ngăn lỗi vòng lặp & Lazy loading khi lấy thông tin người bán
    // Chỉ cho phép lấy các trường đơn giản, chặn các trường quan hệ phức tạp
    @JsonIgnoreProperties({"passwordHash", "wallet", "inventory", "userItems", "tokens", "role", "hibernateLazyInitializer", "handler"})
    private User seller;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    private Integer quantity;
    private BigDecimal price;

    @Column(name = "enhance_level")
    private Integer enhanceLevel;

    private String status; // ACTIVE, SOLD

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}