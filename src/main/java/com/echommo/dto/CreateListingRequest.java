package com.echommo.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateListingRequest {
    private Long userItemId; // [FIX] Đổi Integer -> Long để khớp với UserItem mới
    private Integer quantity;
    private BigDecimal price;
}