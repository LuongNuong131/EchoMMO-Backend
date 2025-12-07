package com.echommo.controller;

import com.echommo.entity.UserItem;
import com.echommo.security.JwtUtils;
import com.echommo.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*", maxAge = 3600)
public class InventoryController {

    @Autowired private InventoryService inventoryService;
    @Autowired private JwtUtils jwtUtils;

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String jwt = jwtUtils.parseJwt(request);
        if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
            String username = jwtUtils.getUserNameFromJwtToken(jwt);
            // Trong thực tế nên cache user id, ở đây gọi service tạm hoặc dùng custom authen
            // Để đơn giản, giả sử bạn có cách lấy ID hoặc query từ DB
            return jwtUtils.getUserIdFromToken(jwt);
            // Lưu ý: Bạn cần chắc chắn JwtUtils có hàm trả về ID,
            // nếu không thì dùng UserRepository tìm theo username
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<List<UserItem>> getMyInventory(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request); // Bạn cần tự implement logic lấy ID từ token
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(inventoryService.getInventory(userId));
    }

    @PostMapping("/equip/{userItemId}")
    public ResponseEntity<?> equipItem(@PathVariable Long userItemId, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();

        try {
            inventoryService.equipItem(userId, userItemId);
            return ResponseEntity.ok("Equipped successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/unequip/{userItemId}")
    public ResponseEntity<?> unequipItem(@PathVariable Long userItemId, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();

        try {
            inventoryService.unequipItem(userId, userItemId);
            return ResponseEntity.ok("Unequipped successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}