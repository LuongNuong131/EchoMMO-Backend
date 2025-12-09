//package com.echommo.controller;
//
//import com.echommo.service.GameService;
//import com.echommo.service.EquipmentService;
//import com.echommo.entity.UserItem;
//import com.echommo.entity.User;
//import com.echommo.entity.Character;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/game")
//public class GameController {
//
//    @Autowired
//    private GameService gameService;
//
//    @Autowired
//    private EquipmentService equipmentService;
//
//    // 1. --- Core Game Actions ---
//
//    @GetMapping("/explore/{userId}")
//    public ResponseEntity<Map<String, Object>> explore(@PathVariable Integer userId) {
//        try {
//            return ResponseEntity.ok(gameService.explore(userId));
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
//        }
//    }
//
//    // [LƯU Ý]: Endpoint /rest đã được chuyển hoàn toàn sang SpaController!
//
//    // 2. --- ITEM ENHANCEMENT ---
//
//    @PostMapping("/item/enhance/{itemId}")
//    public ResponseEntity<?> enhanceItem(@PathVariable("itemId") Long userItemId,
//                                         @RequestParam Integer userId) {
//        try {
//            UserItem updatedItem = equipmentService.upgradeItem(userItemId, userId);
//            return ResponseEntity.ok(updatedItem);
//
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body(e.getMessage());
//        } catch (Exception e) {
//            return ResponseEntity.internalServerError().body("Lỗi hệ thống không xác định.");
//        }
//    }
//
//    // 3. --- Inventory & Equip Actions ---
//
//    @GetMapping("/inventory/{userId}")
//    public ResponseEntity<List<UserItem>> getInventory(@PathVariable Integer userId) {
//        return ResponseEntity.ok(gameService.getInventory(userId));
//    }
//
//    @PostMapping("/item/equip/{itemId}")
//    public ResponseEntity<Map<String, Object>> equipItem(@PathVariable("itemId") Long userItemId, @RequestParam Integer userId) {
//        try {
//            return ResponseEntity.ok(gameService.equipItem(userId, userItemId));
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
//        }
//    }
//
//    @PostMapping("/item/unequip/{itemId}")
//    public ResponseEntity<Map<String, Object>> unequipItem(@PathVariable("itemId") Long userItemId, @RequestParam Integer userId) {
//        try {
//            return ResponseEntity.ok(gameService.unequipItem(userId, userItemId));
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
//        }
//    }
//}
package com.echommo.controller;

import com.echommo.entity.User;
import com.echommo.entity.UserItem;
import com.echommo.repository.UserRepository;
import com.echommo.service.CharacterService;
import com.echommo.service.EquipmentService;
import com.echommo.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*")
public class GameController {

    @Autowired
    private GameService gameService;

    @Autowired
    private EquipmentService equipmentService;

    // [QUAN TRỌNG] Inject CharacterService để tạo nhân vật mặc định
    @Autowired
    private CharacterService characterService;

    @Autowired
    private UserRepository userRepo;

    // Helper: Lấy User hiện tại từ Token
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ========================================================
    // 1. API LẤY THÔNG TIN NHÂN VẬT (Khắc phục lỗi Frontend)
    // ========================================================
    @GetMapping("/character")
    public ResponseEntity<?> getMyCharacterInfo() {
        try {
            User user = getCurrentUser();

            // Nếu User chưa có nhân vật -> Tạo mới luôn và trả về
            if (user.getCharacter() == null) {
                return ResponseEntity.ok(characterService.createDefaultCharacter(user));
            }

            // Nếu đã có -> Trả về nhân vật
            return ResponseEntity.ok(user.getCharacter());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "Lỗi Server: " + e.getMessage()));
        }
    }

    // ========================================================
    // 2. CÁC API GAMEPLAY KHÁC
    // ========================================================

    @PostMapping("/explore")
    public ResponseEntity<Map<String, Object>> explore() {
        try {
            return ResponseEntity.ok(gameService.explore(getCurrentUser().getUserId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/inventory")
    public ResponseEntity<List<UserItem>> getInventory() {
        return ResponseEntity.ok(gameService.getInventory(getCurrentUser().getUserId()));
    }

    @PostMapping("/item/equip/{itemId}")
    public ResponseEntity<Map<String, Object>> equipItem(@PathVariable("itemId") Long userItemId) {
        try {
            return ResponseEntity.ok(gameService.equipItem(getCurrentUser().getUserId(), userItemId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/item/unequip/{itemId}")
    public ResponseEntity<Map<String, Object>> unequipItem(@PathVariable("itemId") Long userItemId) {
        try {
            return ResponseEntity.ok(gameService.unequipItem(getCurrentUser().getUserId(), userItemId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/item/enhance/{itemId}")
    public ResponseEntity<?> enhanceItem(@PathVariable("itemId") Long userItemId) {
        try {
            UserItem updatedItem = equipmentService.upgradeItem(userItemId, getCurrentUser().getUserId());
            return ResponseEntity.ok(updatedItem);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}