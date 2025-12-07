package com.echommo.controller;

import com.echommo.entity.User;
import com.echommo.entity.UserItem;
import com.echommo.repository.UserRepository;
import com.echommo.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true") // Cấu hình rõ ràng cho Frontend
public class InventoryController {

    @Autowired private InventoryService inventoryService;
    @Autowired private UserRepository userRepository;

    // Helper lấy User ID từ Security Context
    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check kỹ nếu chưa đăng nhập hoặc Token lỗi
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("Unauthorized: User is not logged in");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return user.getUserId();
    }

    // [FIX] Thêm "/me" để khớp với Frontend gọi axios.get("/inventory/me")
    @GetMapping("/me")
    public ResponseEntity<?> getMyInventory() {
        try {
            Integer userId = getCurrentUserId();
            List<UserItem> inventory = inventoryService.getInventory(userId);
            return ResponseEntity.ok(inventory);
        } catch (RuntimeException e) {
            // Trả về 401 nếu lỗi Auth để Frontend biết đường redirect login
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching inventory");
        }
    }

    @PostMapping("/equip/{userItemId}")
    public ResponseEntity<?> equipItem(@PathVariable Integer userItemId) {
        try {
            Integer userId = getCurrentUserId();
            inventoryService.equipItem(userId, userItemId);
            return ResponseEntity.ok("Equipped successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/unequip/{userItemId}")
    public ResponseEntity<?> unequipItem(@PathVariable Integer userItemId) {
        try {
            Integer userId = getCurrentUserId();
            inventoryService.unequipItem(userId, userItemId);
            return ResponseEntity.ok("Unequipped successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/use/{userItemId}")
    public ResponseEntity<?> useItem(@PathVariable Integer userItemId) {
        try {
            // Giả định service có hàm useItem, nếu chưa có thì cần implement bên Service
            // inventoryService.useItem(userId, userItemId);
            return ResponseEntity.ok("Used item successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}