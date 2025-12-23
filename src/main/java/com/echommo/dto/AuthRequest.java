package com.echommo.dto;
import lombok.Data;
import lombok.NoArgsConstructor; // [FIX] Thêm
import lombok.AllArgsConstructor; // [FIX] Thêm

@Data
@NoArgsConstructor // [FIX] Quan trọng: Cần thiết để Jackson deserialize JSON
@AllArgsConstructor
public class AuthRequest {
    private String username;
    private String password;
    private String email;
    private String fullName;
}