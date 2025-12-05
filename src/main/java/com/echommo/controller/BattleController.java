package com.echommo.controller;

import com.echommo.dto.BattleResult;
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
        try { return ResponseEntity.ok(battleService.startBattle()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping("/attack")
    public ResponseEntity<?> attack(@RequestBody Map<String, Object> payload) {
        try {
            Integer enemyId = (Integer) payload.get("enemyId");
            Integer enemyHp = (Integer) payload.get("enemyHp");
            boolean isParried = payload.containsKey("isParried") ? (Boolean) payload.get("isParried") : false;
            String attackType = payload.containsKey("attackType") ? (String) payload.get("attackType") : "normal";

            return ResponseEntity.ok(battleService.processTurn(enemyId, enemyHp, isParried, attackType));
        } catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping("/skill")
    public ResponseEntity<?> useSkill(@RequestBody Map<String, Integer> payload) {
        try {
            return ResponseEntity.ok(battleService.useSkill(payload.get("enemyId"), payload.get("enemyHp"), payload.get("skillId")));
        } catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @GetMapping("/skills")
    public ResponseEntity<List<Skill>> getSkills() { return ResponseEntity.ok(battleService.getAllSkills()); }
}