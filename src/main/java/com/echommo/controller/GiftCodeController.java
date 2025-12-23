package com.echommo.controller;

import com.echommo.service.GiftCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/giftcode")
public class GiftCodeController {

    @Autowired
    private GiftCodeService giftCodeService;

    @PostMapping("/redeem")
    public ResponseEntity<?> redeem(@RequestBody Map<String, String> payload) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            String code = payload.get("code");

            if (code == null || code.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Vui lòng nhập mã!");
            }

            String message = giftCodeService.redeemCode(code.trim(), username);
            return ResponseEntity.ok(Map.of("message", message));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi server khi nhập mã."));
        }
    }
}