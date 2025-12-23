package com.echommo.controller;

import com.echommo.entity.User;
import com.echommo.service.EquipmentService;
import com.echommo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/equipment")
@CrossOrigin(origins = "*")
public class EquipmentController {

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private UserService userService;

    // [FIXED] Đã đổi Integer -> Long để khớp với Service
    @PostMapping("/enhance/{userItemId}")
    public ResponseEntity<?> enhance(@PathVariable Long userItemId) {
        try {
            return ResponseEntity.ok(equipmentService.enhanceItem(userItemId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // [FIXED] Đã đổi Integer -> Long
    @PostMapping("/evolve/{userItemId}")
    public ResponseEntity<?> evolve(@PathVariable Long userItemId) {
        try {
            return ResponseEntity.ok(equipmentService.evolveToMythic(userItemId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // [FIXED] Đã đổi Integer -> Long
    @PostMapping("/enhance-mythic-star/{userItemId}")
    public ResponseEntity<?> enhanceStars(@PathVariable Long userItemId, Authentication auth) {
        try {
            User user = userService.getUserFromAuth(auth);
            return ResponseEntity.ok(equipmentService.enhanceMythicStars(userItemId, user.getUserId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}