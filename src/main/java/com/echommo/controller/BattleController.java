package com.echommo.controller;

import com.echommo.service.BattleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
            e.printStackTrace(); // In lỗi ra Terminal của Server để xem
            return ResponseEntity.badRequest().body("Lỗi Server: " + e.toString());
        }
    }

    @PostMapping("/action")
    public ResponseEntity<?> performAction(@RequestBody Map<String, String> payload) {
        try {
            String action = payload.getOrDefault("action", "ATTACK");
            return ResponseEntity.ok(battleService.processTurn(action));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi Action: " + e.toString());
        }
    }

    @PostMapping("/attack")
    public ResponseEntity<?> attack(@RequestBody Map<String, Object> payload) {
        try {
            return ResponseEntity.ok(battleService.attackEnemy(payload));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.toString());
        }
    }
}