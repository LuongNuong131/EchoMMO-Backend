package com.echommo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private Integer id;
    private String username;
    private String email;
    private String role;
    private BigDecimal echoCoin;
    private BigDecimal gold; // [FIX] Đổi Long -> BigDecimal
    private String avatarUrl;

    public AuthResponse(String accessToken, Integer id, String username, String email, String role, BigDecimal echoCoin, BigDecimal gold, String avatarUrl) {
        this.token = accessToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.echoCoin = echoCoin;
        this.gold = gold;
        this.avatarUrl = avatarUrl;
    }
}