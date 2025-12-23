package com.echommo.dto;

import lombok.Data;

@Data
public class BuyItemRequest {
    private Integer itemId; // [FIXED] Đổi sang Integer
    private Integer quantity;
}