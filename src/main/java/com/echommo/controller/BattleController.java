package com.echommo.controller;

import com.echommo.entity.Skill;
import com.echommo.service.BattleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/battle")
public class BattleController {

    @Autowired private BattleService battleService;

    @PostMapping("/start")
    public ResponseEntity<?> startBattle() {
        try {
            return ResponseEntity.ok(battleService.startBattle());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // [FIX] Gọi hàm attackEnemy thay vì processTurn
    @PostMapping("/attack")
    public ResponseEntity<?> attack(@RequestBody Map<String, Object> payload) {
        try {
            // Payload nhận từ frontend: { enemyId, enemyHp, isBuffed }
            return ResponseEntity.ok(battleService.attackEnemy(payload));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/skill")
    public ResponseEntity<?> useSkill(@RequestBody Map<String, Object> payload) {
        try {
            // Chuyển tiếp sang logic đánh thường nếu chưa implement skill riêng biệt
            // Hoặc map payload này vào attackEnemy nếu muốn tái sử dụng logic buff
            return ResponseEntity.ok(battleService.attackEnemy(payload));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/skills")
    public ResponseEntity<List<Skill>> getSkills() {
        // Hàm này bây giờ sẽ hoạt động vì BattleService đã có getAllSkills()
        return ResponseEntity.ok(battleService.getAllSkills());
    }
}