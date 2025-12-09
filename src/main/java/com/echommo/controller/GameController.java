package com.echommo.controller;

import com.echommo.service.GameService;
import com.echommo.service.EquipmentService; // <-- [FIX] Import EquipmentService
import com.echommo.entity.UserItem;
import com.echommo.entity.User;
import com.echommo.entity.Character;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/game")
public class GameController {

    @Autowired
    private GameService gameService;

    // [FIX] Inject EquipmentService để xử lý cường hóa
    @Autowired
    private EquipmentService equipmentService;

    // 1. --- Core Game Actions ---

    @GetMapping("/explore/{userId}")
    public ResponseEntity<Map<String, Object>> explore(@PathVariable Integer userId) {
        try {
            return ResponseEntity.ok(gameService.explore(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/rest/{userId}")
    public ResponseEntity<User> restAtInn(@PathVariable Integer userId) {
        try {
            return ResponseEntity.ok(gameService.restAtInn(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 2. --- [FIXED METHOD] ITEM ENHANCEMENT ---

    // Phương thức này đang gọi lỗi (L72) và cần chuyển hướng đến EquipmentService
    @PostMapping("/item/enhance/{itemId}")
    public ResponseEntity<?> enhanceItem(@PathVariable("itemId") Long userItemId,
                                         @RequestParam Integer userId) {
        try {
            // [FIX] Gọi đúng phương thức trong EquipmentService
            UserItem updatedItem = equipmentService.upgradeItem(userItemId, userId);
            return ResponseEntity.ok(updatedItem);

        } catch (RuntimeException e) {
            // RuntimeException bắt lỗi thiếu nguyên liệu, item không chính chủ...
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi hệ thống không xác định.");
        }
    }

    // 3. --- Inventory & Equip Actions ---

    @GetMapping("/inventory/{userId}")
    public ResponseEntity<List<UserItem>> getInventory(@PathVariable Integer userId) {
        return ResponseEntity.ok(gameService.getInventory(userId));
    }

    @PostMapping("/item/equip/{itemId}")
    public ResponseEntity<Map<String, Object>> equipItem(@PathVariable("itemId") Long userItemId, @RequestParam Integer userId) {
        try {
            // Giả định gameService có logic equip
            return ResponseEntity.ok(gameService.equipItem(userId, userItemId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/item/unequip/{itemId}")
    public ResponseEntity<Map<String, Object>> unequipItem(@PathVariable("itemId") Long userItemId, @RequestParam Integer userId) {
        try {
            // Giả định gameService có logic unequip
            return ResponseEntity.ok(gameService.unequipItem(userId, userItemId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}