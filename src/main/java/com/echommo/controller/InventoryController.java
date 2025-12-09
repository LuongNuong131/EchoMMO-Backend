package com.echommo.controller;

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

    // Helper lấy User ID từ Token
    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("Unauthorized: Bạn chưa đăng nhập");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getUserId();
    }

    // Lấy danh sách đồ (Túi đồ + Đồ đang mặc)
    @GetMapping("/me")
    public ResponseEntity<?> getMyInventory() {
        try {
            return ResponseEntity.ok(inventoryService.getInventory(getCurrentUserId()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi lấy túi đồ: " + e.getMessage());
        }
    }

    // Mặc đồ (Equip)
    @PostMapping("/equip/{userItemId}")
    public ResponseEntity<?> equipItem(@PathVariable Long userItemId) {
        try {
            // Gọi Service để xử lý logic mặc đồ (tự tháo đồ cũ ra nếu trùng slot)
            inventoryService.equipItem(getCurrentUserId(), userItemId);
            return ResponseEntity.ok("Đã trang bị vật phẩm thành công!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Tháo đồ (Unequip)
    @PostMapping("/unequip/{userItemId}")
    public ResponseEntity<?> unequipItem(@PathVariable Long userItemId) {
        try {
            inventoryService.unequipItem(getCurrentUserId(), userItemId);
            return ResponseEntity.ok("Đã tháo trang bị!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Cường hóa (Enhance)
    @PostMapping("/enhance/{userItemId}")
    public ResponseEntity<?> enhanceItem(@PathVariable Long userItemId) {
        try {
            // Trả về item sau khi cường hóa (kèm thông báo thành công/thất bại trong object trả về nếu cần)
            return ResponseEntity.ok(inventoryService.enhanceItem(getCurrentUserId(), userItemId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}