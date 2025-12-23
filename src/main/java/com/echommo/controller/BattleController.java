package com.echommo.controller;

import com.echommo.dto.BattleAttackRequest; // [QUAN TRỌNG] Import DTO này
import com.echommo.dto.BattleResult;
import com.echommo.service.BattleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/battle")
@CrossOrigin(origins = "http://localhost:5173") // Cho phép Frontend gọi
public class BattleController {

    @Autowired
    private BattleService battleService;

    // 1. Bắt đầu trận đấu
    @PostMapping("/start")
    public ResponseEntity<BattleResult> startBattle() {
        return ResponseEntity.ok(battleService.startBattle());
    }

    // 2. Tấn công tự động (Auto Turn)
    // [FIX] Sửa để nhận BattleAttackRequest thay vì Map
    @PostMapping("/attack")
    public ResponseEntity<BattleResult> attack(@RequestBody BattleAttackRequest req) {
        return ResponseEntity.ok(battleService.processTurn(req));
    }

    // 3. Thực hiện hành động cụ thể (QTE)
    // [FIX] Service giờ yêu cầu BattleAttackRequest, nên ta tạo object bọc lại
    @PostMapping("/action")
    public ResponseEntity<BattleResult> action(@RequestBody Map<String, String> payload) {
        BattleAttackRequest req = new BattleAttackRequest();
        // Ở đây có thể set logic riêng nếu cần, ví dụ: req.setIsBuffed(true) nếu action là BLOCK
        return ResponseEntity.ok(battleService.processTurn(req));
    }
}