package com.echommo.controller;

import com.echommo.dto.GatherRequest;
import com.echommo.entity.User;
import com.echommo.repository.UserRepository;
import com.echommo.service.ExplorationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/exploration")
@CrossOrigin(origins = "*")
public class ExplorationController {

    @Autowired private ExplorationService explorationService;
    @Autowired private UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
    }

    // 1. Explore
    @PostMapping("/explore")
    public ResponseEntity<?> explore(@RequestBody Map<String, String> req) {
        try {
            // Nhận trực tiếp chuỗi mã map (Ví dụ: "MAP_01")
            String mapCode = req.get("mapId");

            // Fallback nếu không có mã map thì mặc định MAP_01
            if (mapCode == null || mapCode.isEmpty()) {
                mapCode = "MAP_01";
            }

            // Gọi service với mã map chuẩn
            return ResponseEntity.ok(explorationService.explore(getCurrentUser(), mapCode));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 2. Gather - [FIX] Sử dụng DTO để tránh lỗi ép kiểu JSON (400 Bad Request)
    @PostMapping("/gather")
    public ResponseEntity<?> gather(@RequestBody GatherRequest req) {
        try {
            if (req.getItemId() == null) {
                throw new RuntimeException("Thiếu Item ID");
            }

            // Mặc định amount là 1 nếu null hoặc <= 0
            int amount = (req.getAmount() != null && req.getAmount() > 0) ? req.getAmount() : 1;

            return ResponseEntity.ok(explorationService.gatherResource(getCurrentUser(), req.getItemId(), amount));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}