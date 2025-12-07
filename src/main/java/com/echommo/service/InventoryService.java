package com.echommo.service;

import com.echommo.entity.UserItem;
import com.echommo.repository.UserItemRepository;
import com.echommo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class InventoryService {

    @Autowired
    private UserItemRepository userItemRepo;

    @Autowired
    private UserRepository userRepo;

    // Lấy danh sách đồ
    public List<UserItem> getInventory(Integer userId) { // [FIX] Long -> Integer
        return userItemRepo.findByUser_UserId(userId);
    }

    // --- LOGIC MẶC TRANG BỊ ---
    @Transactional
    public void equipItem(Integer userId, Integer userItemId) { // [FIX] Long -> Integer
        // 1. Tìm món đồ cần mặc
        UserItem newItem = userItemRepo.findById(userItemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        // 2. Check quyền sở hữu
        if (!newItem.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("This item does not belong to you!");
        }

        // 3. Lấy loại item (WEAPON, ARMOR, HELMET...)
        String itemType = newItem.getItem().getType();

        // 4. Tìm xem user có đang mặc món nào cùng loại không?
        Optional<UserItem> currentEquipped = userItemRepo
                .findByUser_UserIdAndItem_TypeAndIsEquippedTrue(userId, itemType);

        // 5. Nếu có -> Tháo món cũ ra
        if (currentEquipped.isPresent()) {
            UserItem oldItem = currentEquipped.get();
            // Nếu chính là món đang click thì bỏ qua (hoặc coi như tháo ra mặc lại)
            if (!oldItem.getUserItemId().equals(newItem.getUserItemId())) {
                oldItem.setIsEquipped(false);
                userItemRepo.save(oldItem);
            }
        }

        // 6. Mặc món mới vào
        newItem.setIsEquipped(true);
        userItemRepo.save(newItem);
    }

    // --- LOGIC THÁO TRANG BỊ ---
    @Transactional
    public void unequipItem(Integer userId, Integer userItemId) { // [FIX] Long -> Integer
        UserItem item = userItemRepo.findById(userItemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (!item.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Not your item");
        }

        // Đơn giản là set false
        item.setIsEquipped(false);
        userItemRepo.save(item);
    }
}