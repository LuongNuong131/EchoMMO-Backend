package com.echommo.controller;

import com.echommo.entity.UserItem;
import com.echommo.service.EquipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    @Autowired
    private EquipmentService equipmentService;

    // 1. Nâng cấp thường (+1)
    @PostMapping("/{itemId}/upgrade")
    public ResponseEntity<?> upgradeItem(@PathVariable("itemId") Long userItemId,
                                         @RequestParam Integer userId) { // <-- [FIXED] Thêm userId
        try {
            UserItem updatedItem = equipmentService.upgradeItem(userItemId, userId);
            return ResponseEntity.ok(updatedItem);
        } catch (Exception e) {
            // Trả về lỗi 400 nếu thiếu nguyên liệu/logic lỗi
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 2. Tiến hóa Mythic (5% Chance)
    @PostMapping("/{itemId}/evolve-mythic")
    public ResponseEntity<String> evolveToMythic(@PathVariable("itemId") Long userItemId,
                                                 @RequestParam Integer userId) { // <-- [FIXED] Thêm userId
        try {
            // Service sẽ trả về message "Thành công" hoặc "Thất bại"
            String result = equipmentService.evolveToMythic(userItemId, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 3. Nâng cấp Mythic Level (+1)
    // Method này trong Service KHÔNG cần userId vì không trừ nguyên liệu chung (chỉ cần item id)
    @PostMapping("/{itemId}/upgrade-mythic")
    public ResponseEntity<?> upgradeMythic(@PathVariable("itemId") Long userItemId) {
        try {
            UserItem updatedItem = equipmentService.upgradeMythicLevel(userItemId);
            return ResponseEntity.ok(updatedItem);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}