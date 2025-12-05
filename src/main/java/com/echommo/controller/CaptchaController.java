package com.echommo.controller;

import com.echommo.dto.CaptchaResponse;
import com.echommo.service.CaptchaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    @Autowired private CaptchaService captchaService;

    @GetMapping("/generate")
    public ResponseEntity<CaptchaResponse> generate() {
        return ResponseEntity.ok(captchaService.generateCaptcha());
    }

    @PostMapping("/solve")
    public ResponseEntity<?> solve(@RequestBody Map<String, Integer> payload) {
        try {
            Integer itemId = payload.get("itemId");
            String result = captchaService.solveCaptcha(itemId);
            if (result.startsWith("Sai")) {
                return ResponseEntity.badRequest().body(result);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi xử lý");
        }
    }
}