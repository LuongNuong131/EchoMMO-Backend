package com.echommo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor // Thêm cái này cho chắc cốp, nhỡ đâu cần dùng
public class LeaderboardEntry {
    private String username;    // Tên nhân vật
    private String value;       // Giá trị hiển thị (VD: "Lv 10" hoặc "1,000 Vàng")
    private String rank;        // Hạng (VD: "1")
    private String avatar;      // Avatar (VD: "🐲")
}