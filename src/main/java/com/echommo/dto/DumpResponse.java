package com.echommo.dto;

import com.echommo.enums.Rarity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DumpResponse {
    private boolean success;
    private String message;
    private BigDecimal totalGold;
    private BigDecimal totalEcho;

    private List<RewardItem> receivedItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RewardItem {
        private String code;     // Mã vật phẩm
        private String name;     // Tên vật phẩm
        private String image;    // [NEW] Đường dẫn ảnh chính xác từ Database
        private Rarity rarity;   // Độ hiếm
        private int quantity;    // Số lượng
    }
}