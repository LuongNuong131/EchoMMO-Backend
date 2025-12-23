package com.echommo.service;

import com.echommo.entity.User;
import com.echommo.entity.UserItem;
import java.util.List;

public interface InventoryService {
    List<UserItem> getInventory(Integer charId);

    // [FIXED] Long userItemId
    void equipItem(Integer charId, Long userItemId);
    void unequipItem(Integer charId, Long userItemId);
    UserItem enhanceItem(Integer charId, Long userItemId);

    User expandInventory(User user);
    void addItemToInventory(User user, Integer itemId, int quantity);

    // [FIXED] Long userItemId
    UserItem repairItem(User user, Long userItemId);
}