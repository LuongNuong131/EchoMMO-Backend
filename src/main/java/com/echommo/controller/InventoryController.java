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
// [FIX] Xóa dòng cũ, thay bằng dòng này (chấp nhận mọi nguồn):
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class InventoryController {

    @Autowired private InventoryService inventoryService;
    @Autowired private UserRepository userRepository;

    // ... (Phần code dưới giữ nguyên không cần sửa) ...

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("Unauthorized: User is not logged in");
        }
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return user.getUserId();
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyInventory() {
        try {
            Integer userId = getCurrentUserId();
            return ResponseEntity.ok(inventoryService.getInventory(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching inventory");
        }
    }

    // ... Các hàm equip/unequip giữ nguyên ...
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
            return ResponseEntity.ok("Used item successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}