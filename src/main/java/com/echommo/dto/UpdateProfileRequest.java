package com.echommo.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String username;
    private String email;
    private String password;
    private String avatarUrl;       // Skin game
    private String profileImageUrl; // Ảnh thật upload
}