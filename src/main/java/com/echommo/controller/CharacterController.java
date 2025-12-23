package com.echommo.controller;

import com.echommo.dto.CharacterRequest;
import com.echommo.service.CharacterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/character")
public class CharacterController {

    @Autowired private CharacterService s;

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        return ResponseEntity.ok(s.getMyCharacter());
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody CharacterRequest r) {
        try {
            return ResponseEntity.ok(s.createCharacter(r));
        } catch(Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 👇 API: Cộng điểm tiềm năng
    // Body mẫu: { "str": 1, "vit": 2, "agi": 0, "dex": 0, "int": 0, "luck": 0 }
    @PostMapping("/add-stats")
    public ResponseEntity<?> addStats(@RequestBody Map<String, Integer> stats) {
        try {
            // Gọi Service để xử lý logic cộng điểm và lưu DB
            return ResponseEntity.ok(s.addStats(stats));
        } catch (IllegalArgumentException e) {
            // Lỗi do người dùng gửi số âm hoặc quá điểm
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            // Lỗi hệ thống khác
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}