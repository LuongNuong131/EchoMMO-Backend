package com.echommo.controller;

import com.echommo.entity.User;
import com.echommo.entity.UserItem;
import com.echommo.repository.UserRepository;
import com.echommo.service.InventoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*", maxAge = 3600)
public class InventoryController {

    @Autowired private InventoryService inventoryService;
    @Autowired private UserRepository userRepository; // [FIX] Thêm Repo để tìm User ID

    // [FIX] Helper method chuẩn để lấy UserID từ Security Context
    private Integer getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getUserId();
    }

    @GetMapping
    public ResponseEntity<List<UserItem>> getMyInventory() {
        try {
            Integer userId = getCurrentUserId();
            return ResponseEntity.ok(inventoryService.getInventory(userId));
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/equip/{userItemId}")
    public ResponseEntity<?> equipItem(@PathVariable Integer userItemId) { // [FIX] Long -> Integer
        try {
            Integer userId = getCurrentUserId();
            inventoryService.equipItem(userId, userItemId);
            return ResponseEntity.ok("Equipped successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/unequip/{userItemId}")
    public ResponseEntity<?> unequipItem(@PathVariable Integer userItemId) { // [FIX] Long -> Integer
        try {
            Integer userId = getCurrentUserId();
            inventoryService.unequipItem(userId, userItemId);
            return ResponseEntity.ok("Unequipped successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}