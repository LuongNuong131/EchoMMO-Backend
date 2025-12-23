package com.echommo.controller;

import com.echommo.entity.User;
import com.echommo.repository.UserRepository;
import com.echommo.service.SpaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/spa")
public class SpaController {

    @Autowired private SpaService spaService;
    @Autowired private UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping("/relax")
    public ResponseEntity<?> relax(@RequestBody Map<String, String> payload) {
        try {
            // Lấy loại gói (STANDARD/VIP) từ body request
            String type = payload.get("type");
            if (type == null) type = "STANDARD"; // Mặc định

            User user = getCurrentUser();

            // [FIX] Gọi đúng hàm useSpa của Service
            return ResponseEntity.ok(spaService.useSpa(user, type));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}