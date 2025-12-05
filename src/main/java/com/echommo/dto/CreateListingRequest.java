package com.echommo.dto;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateListingRequest {
    private Integer userItemId; // ID món đồ trong túi muốn bán
    private BigDecimal price;   // Giá muốn bán
    private Integer quantity;   // Số lượng (mặc định 1)
}