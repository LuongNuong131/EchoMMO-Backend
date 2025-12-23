package com.echommo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_listings")
@Data
public class MarketListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "listing_id")
    private Long listingId;

    // [FIX 1] Giữ nguyên quan hệ UserItem đã sửa cho đúng DB
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_item_id", referencedColumnName = "user_item_id", nullable = false)
    private UserItem userItem;

    // [RESTORED] Khôi phục quan hệ Seller (User) để Service gọi được .getSeller()
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    // [RESTORED] Khôi phục quan hệ Item (để hiển thị icon/tên nhanh)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    // [RESTORED] Khôi phục field số lượng
    @Column(name = "quantity")
    private Integer quantity;

    // [FIX 2] Đổi sang BigDecimal để khớp với Service và Wallet
    @Column(name = "price", nullable = false)
    private BigDecimal price;

    // [RESTORED] Trạng thái: ACTIVE, SOLD, CANCELLED
    @Column(name = "status")
    private String status;

    // [RESTORED] Loại tiền tệ: GOLD, ECHO_COIN...
    @Column(name = "currency_type")
    private String currencyType;

    @Column(name = "listed_at")
    private LocalDateTime listedAt;

    @PrePersist
    protected void onCreate() {
        listedAt = LocalDateTime.now();
    }
}