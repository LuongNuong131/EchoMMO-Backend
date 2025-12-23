package com.echommo.enums;

import java.math.BigDecimal;

public enum SpaPackage {
    // Gói Thường: Mặc định 120s
    BASIC("Gói Cơ Bản", BigDecimal.ZERO, 120),

    // Gói VIP: Mặc định 10s
    VIP("Gói Thượng Hạng", new BigDecimal("0.5"), 10);

    private final String name;
    private final BigDecimal cost; // Giá base (sẽ được tính lại trong Service)
    private final int durationSeconds;

    SpaPackage(String name, BigDecimal cost, int durationSeconds) {
        this.name = name;
        this.cost = cost;
        this.durationSeconds = durationSeconds;
    }

    public String getName() { return name; }
    public BigDecimal getBaseCost() { return cost; }
    public int getDurationSeconds() { return durationSeconds; }
}