package com.echommo.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateListingRequest {
    private Long userItemId; // [FIXED] Dùng Long
    private Integer quantity;
    private BigDecimal price;
}