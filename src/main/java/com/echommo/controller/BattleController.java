package com.echommo.controller;

import com.echommo.entity.Skill;
import com.echommo.service.BattleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

<<<<<<< HEAD
//@RestController
//@RequestMapping("/api/battle")
//public class BattleController {
//
//    @Autowired
//    private BattleService battleService;
//
//    @PostMapping("/start")
//    public ResponseEntity<?> startBattle() {
//        try {
//            return ResponseEntity.ok(battleService.startBattle());
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(e.getMessage());
//        }
//    }
//
//    // UPDATE: Nhận thêm attackType và isParried
//    @PostMapping("/attack")
//    public ResponseEntity<?> attack(@RequestBody Map<String, Object> payload) {
//        try {
//            Integer enemyId = (Integer) payload.get("enemyId");
//            Integer enemyHp = (Integer) payload.get("enemyHp");
//
//            // Mặc định false/normal nếu không có
//            boolean isParried = payload.containsKey("isParried") ? (Boolean) payload.get("isParried") : false;
//            String attackType = payload.containsKey("attackType") ? (String) payload.get("attackType") : "normal";
//
//            return ResponseEntity.ok(battleService.processTurn(enemyId, enemyHp, isParried, attackType));
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.badRequest().body(e.getMessage());
//        }
//    }
//
//    @PostMapping("/skill")
//    public ResponseEntity<?> useSkill(@RequestBody Map<String, Integer> payload) {
//        try {
//            Integer enemyId = payload.get("enemyId");
//            Integer enemyHp = payload.get("enemyHp");
//            Integer skillId = payload.get("skillId");
//            return ResponseEntity.ok(battleService.useSkill(enemyId, enemyHp, skillId));
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(e.getMessage());
//        }
//    }
//
//    @GetMapping("/skills")
//    public ResponseEntity<List<Skill>> getSkills() {
//        return ResponseEntity.ok(battleService.getAllSkills());
//    }
//}

=======
>>>>>>> 31f4b17a4f519d2a38168af40e596afb5316f91a
@RestController
@RequestMapping("/api/battle")
public class BattleController {

    @Autowired
    private BattleService battleService;

    @PostMapping("/start")
    public ResponseEntity<?> startBattle() {
        try {
            return ResponseEntity.ok(battleService.startBattle());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

<<<<<<< HEAD
=======
    // UPDATE: Nhận thêm attackType và isParried
>>>>>>> 31f4b17a4f519d2a38168af40e596afb5316f91a
    @PostMapping("/attack")
    public ResponseEntity<?> attack(@RequestBody Map<String, Object> payload) {
        try {
            Integer enemyId = (Integer) payload.get("enemyId");
            Integer enemyHp = (Integer) payload.get("enemyHp");

<<<<<<< HEAD
            // Lấy thêm tham số từ Game-Fi Logic
=======
            // Mặc định false/normal nếu không có
>>>>>>> 31f4b17a4f519d2a38168af40e596afb5316f91a
            boolean isParried = payload.containsKey("isParried") ? (Boolean) payload.get("isParried") : false;
            String attackType = payload.containsKey("attackType") ? (String) payload.get("attackType") : "normal";

            return ResponseEntity.ok(battleService.processTurn(enemyId, enemyHp, isParried, attackType));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/skill")
    public ResponseEntity<?> useSkill(@RequestBody Map<String, Integer> payload) {
        try {
            Integer enemyId = payload.get("enemyId");
            Integer enemyHp = payload.get("enemyHp");
            Integer skillId = payload.get("skillId");
            return ResponseEntity.ok(battleService.useSkill(enemyId, enemyHp, skillId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/skills")
    public ResponseEntity<List<Skill>> getSkills() {
        return ResponseEntity.ok(battleService.getAllSkills());
    }
}