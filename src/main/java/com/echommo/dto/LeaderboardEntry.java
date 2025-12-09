package com.echommo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LeaderboardEntry {
    private String username;    // Tên nhân vật
    private String value;   // Giá trị hiển thị (Lv 10 hoặc 1000 Vàng)
    private String rank;    // Hạng
    private String avatar;  // Chữ cái đầu tên
}