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

    // 👇 API MỚI: Cộng điểm tiềm năng (STR, VIT, AGI)
    // Body gửi lên: { "str": 1, "vit": 0, "agi": 2 }
    @PostMapping("/add-stats")
    public ResponseEntity<?> addStats(@RequestBody Map<String, Integer> stats) {
        try {
            int str = stats.getOrDefault("str", 0);
            int vit = stats.getOrDefault("vit", 0);
            int agi = stats.getOrDefault("agi", 0);

            // Bạn cần thêm hàm addStats vào CharacterService
            // return ResponseEntity.ok(s.addStats(str, vit, agi));
            return ResponseEntity.ok("API OK. Need Service Impl.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}