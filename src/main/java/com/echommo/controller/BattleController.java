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

    // Bắt đầu trận đấu mới
    @PostMapping("/start")
    public ResponseEntity<?> startBattle() {
        try {
            return ResponseEntity.ok(battleService.startBattle());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Thực hiện lượt tiếp theo (Hoặc hành động Đỡ đòn)
    @PostMapping("/action")
    public ResponseEntity<?> performAction(@RequestBody Map<String, String> payload) {
        try {
            // Payload: { "action": "ATTACK" } hoặc { "action": "BLOCK" }
            String action = payload.getOrDefault("action", "ATTACK");
            return ResponseEntity.ok(battleService.processTurn(action));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}