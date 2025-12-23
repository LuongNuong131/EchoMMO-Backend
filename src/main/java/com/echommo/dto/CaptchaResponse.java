package com.echommo.dto;

import com.echommo.entity.Item;
import lombok.Data;
import java.util.List;

@Data
public class CaptchaResponse {
    private String question; // VD: "Hãy chọn Kiếm Gỗ"
    private List<Item> options; // Danh sách 3 món để chọn
}