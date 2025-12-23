package com.echommo.dto;

public class SellItemRequest {
    private Long userItemId; // [FIXED] Dùng Long
    private int quantity;

    public SellItemRequest() {
    }

    public SellItemRequest(Long userItemId, int quantity) {
        this.userItemId = userItemId;
        this.quantity = quantity;
    }

    public Long getUserItemId() {
        return userItemId;
    }

    public void setUserItemId(Long userItemId) {
        this.userItemId = userItemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}