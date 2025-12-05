package com.echommo.dto;
import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String password;
    private String email; // Dùng cho đăng ký
    private String fullName; // Dùng cho đăng ký
}