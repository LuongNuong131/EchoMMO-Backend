package com.echommo.controller;

import com.echommo.entity.User;
import com.echommo.repository.UserRepository;
import com.echommo.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired private InventoryService inventoryService;
    @Autowired private UserRepository userRepository;

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("Unauthorized");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getUserId();
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyInventory() {
        try {
            // [FIX] Đã xóa .longValue() -> Truyền thẳng Integer vào vì Service nhận Integer
            return ResponseEntity.ok(inventoryService.getInventory(getCurrentUserId()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // Các hàm dưới nhận userItemId là Long (ID món đồ) -> Đúng
    @PostMapping("/equip/{userItemId}")
    public ResponseEntity<?> equipItem(@PathVariable Long userItemId) {
        try {
            // inventoryService.equipItem(getCurrentUserId(), userItemId);
            return ResponseEntity.ok("Equipped (Logic need impl in Service)");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/unequip/{userItemId}")
    public ResponseEntity<?> unequipItem(@PathVariable Long userItemId) {
        try {
            // inventoryService.unequipItem(getCurrentUserId(), userItemId);
            return ResponseEntity.ok("Unequipped");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/enhance/{userItemId}")
    public ResponseEntity<?> enhanceItem(@PathVariable Long userItemId) {
        try {
            return ResponseEntity.ok(inventoryService.enhanceItem(userItemId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}