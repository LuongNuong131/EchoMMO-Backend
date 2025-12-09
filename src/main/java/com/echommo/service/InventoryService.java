package com.echommo.service;

import com.echommo.entity.UserItem;
import java.util.List;

public interface InventoryService {
    // Lấy danh sách item của user
    List<UserItem> getInventory(Integer userId);

    // Mặc đồ: Cần userId để check chủ sở hữu
    void equipItem(Integer userId, Long userItemId);

    // Tháo đồ
    void unequipItem(Integer userId, Long userItemId);

    // Cường hóa: Trả về item đã update
    UserItem enhanceItem(Integer userId, Long userItemId);
}