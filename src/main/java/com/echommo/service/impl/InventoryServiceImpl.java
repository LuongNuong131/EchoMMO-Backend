package com.echommo.service.impl;

import com.echommo.entity.Item;
import com.echommo.entity.UserItem;
import com.echommo.repository.UserItemRepository;
import com.echommo.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
public class InventoryServiceImpl implements InventoryService {

    @Autowired
    private UserItemRepository userItemRepository;

    @Override
    public List<UserItem> getInventory(Integer userId) {
        // Lấy tất cả item của user (bao gồm đang mặc và trong túi)
        return userItemRepository.findByUser_UserId(userId);
    }

    @Override
    @Transactional
    public void equipItem(Integer userId, Long userItemId) {
        // 1. Tìm món đồ cần mặc
        UserItem itemToEquip = userItemRepository.findById(userItemId)
                .orElseThrow(() -> new RuntimeException("Vật phẩm không tồn tại"));

        // 2. Check quyền sở hữu
        if (!itemToEquip.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Vật phẩm này không thuộc về bạn!");
        }

        // 3. Lấy thông tin loại trang bị (Ví dụ: WEAPON, ARMOR, HELMET...)
        Item baseItem = itemToEquip.getItem();
        String slotType = baseItem.getType(); // Giả sử trong Entity Item có trường 'type'

        // 4. Tìm xem user có đang mặc món nào cùng loại (cùng slot) không?
        List<UserItem> currentInventory = userItemRepository.findByUser_UserId(userId);

        for (UserItem ui : currentInventory) {
            // Nếu đang mặc (isEquipped = true) VÀ cùng loại (Type giống nhau)
            if (Boolean.TRUE.equals(ui.getIsEquipped()) && ui.getItem().getType().equals(slotType)) {
                // Tháo món cũ ra
                ui.setIsEquipped(false);
                userItemRepository.save(ui);
            }
        }

        // 5. Mặc món mới vào
        itemToEquip.setIsEquipped(true);
        userItemRepository.save(itemToEquip);
    }

    @Override
    @Transactional
    public void unequipItem(Integer userId, Long userItemId) {
        UserItem itemToUnequip = userItemRepository.findById(userItemId)
                .orElseThrow(() -> new RuntimeException("Vật phẩm không tồn tại"));

        if (!itemToUnequip.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Không phải đồ của bạn!");
        }

        if (!Boolean.TRUE.equals(itemToUnequip.getIsEquipped())) {
            throw new RuntimeException("Vật phẩm này chưa được trang bị!");
        }

        itemToUnequip.setIsEquipped(false);
        userItemRepository.save(itemToUnequip);
    }

    @Override
    @Transactional
    public UserItem enhanceItem(Integer userId, Long userItemId) {
        UserItem item = userItemRepository.findById(userItemId)
                .orElseThrow(() -> new RuntimeException("Vật phẩm không tồn tại"));

        if (!item.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Không chính chủ!");
        }

        // --- LOGIC CƯỜNG HÓA ---
        // Giả sử: Tỉ lệ thành công giảm dần theo cấp độ hiện tại
        int currentLevel = item.getEnhancementLevel() != null ? item.getEnhancementLevel() : 0;
        int successChance = 100 - (currentLevel * 10); // Ví dụ: Cấp 0=100%, Cấp 1=90%, Cấp 5=50%
        if (successChance < 10) successChance = 10; // Tối thiểu 10%

        Random rand = new Random();
        int roll = rand.nextInt(100); // 0 -> 99

        if (roll < successChance) {
            // Thành công
            item.setEnhancementLevel(currentLevel + 1);
            // Tăng chỉ số (Ví dụ: +10% sức mạnh mỗi cấp)
            // item.setAttackPower(...) -> Tùy logic game của bạn
        } else {
            // Thất bại (Có thể trừ vàng, hoặc giảm cấp nếu muốn hardcore)
            throw new RuntimeException("Cường hóa thất bại! (Tỉ lệ: " + successChance + "%)");
        }

        return userItemRepository.save(item);
    }
}