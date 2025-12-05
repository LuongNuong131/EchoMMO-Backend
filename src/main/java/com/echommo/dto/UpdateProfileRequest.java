package com.echommo.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String avatarUrl; // Lưu đường dẫn ảnh hoặc mã avatar
}