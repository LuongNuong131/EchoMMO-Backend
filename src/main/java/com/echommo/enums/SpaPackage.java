package com.echommo.enums;

import java.math.BigDecimal;

public enum SpaPackage {
    STANDARD("Vé Thường", new BigDecimal("50"), "GOLD", 0.5),   // 50 Vàng, hồi 50%
    VIP("Vé VIP", new BigDecimal("200"), "GOLD", 1.0),          // 200 Vàng, hồi 100%
    ROYAL("Vé Hoàng Gia", new BigDecimal("10"), "DIAMOND", 1.0); // 10 KC, hồi 100%

    private final String name;
    private final BigDecimal cost;
    private final String currencyType;
    private final double recoveryRate;

    SpaPackage(String name, BigDecimal cost, String currencyType, double recoveryRate) {
        this.name = name;
        this.cost = cost;
        this.currencyType = currencyType;
        this.recoveryRate = recoveryRate;
    }

    // Getters
    public String getName() { return name; }
    public BigDecimal getCost() { return cost; }
    public String getCurrencyType() { return currencyType; }
    public double getRecoveryRate() { return recoveryRate; }
}