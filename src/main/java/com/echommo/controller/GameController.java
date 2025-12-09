package com.echommo.controller;

import com.echommo.entity.User;
import com.echommo.entity.UserItem;
import com.echommo.service.BattleService;
import com.echommo.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "http://localhost:5173")
public class GameController {

    @Autowired private GameService gameService;
    @Autowired private BattleService battleService;

    // --- PLAYER & INFO ---

    @GetMapping("/player/{id}")
    public ResponseEntity<User> getPlayer(@PathVariable Integer id) {
        return ResponseEntity.ok(gameService.getPlayerOrCreate(id));
    }

    @GetMapping("/inventory/{playerId}")
    public ResponseEntity<List<UserItem>> getInventory(@PathVariable Integer playerId) {
        return ResponseEntity.ok(gameService.getInventory(playerId));
    }

    // --- GAME ACTIONS ---

    @PostMapping("/explore")
    public ResponseEntity<Map<String, Object>> explore(@RequestParam Integer playerId) {
        return ResponseEntity.ok(gameService.explore(playerId));
    }

    @PostMapping("/rest")
    public ResponseEntity<User> rest(@RequestParam Integer playerId) {
        try {
            return ResponseEntity.ok(gameService.restAtInn(playerId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build(); // Hoặc trả về message lỗi
        }
    }

    @PostMapping("/equip")
    public ResponseEntity<Map<String, Object>> equip(@RequestParam Integer playerId, @RequestParam Long itemId) {
        try {
            return ResponseEntity.ok(gameService.equipItem(playerId, itemId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/unequip")
    public ResponseEntity<Map<String, Object>> unequip(@RequestParam Integer playerId, @RequestParam Long itemId) {
        try {
            return ResponseEntity.ok(gameService.unequipItem(playerId, itemId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // [BỔ SUNG] Endpoint Enhance mà Service có nhưng Controller bị thiếu
    @PostMapping("/enhance")
    public ResponseEntity<?> enhance(@RequestParam Integer playerId, @RequestParam Long itemId) {
        try {
            return ResponseEntity.ok(gameService.enhanceItem(playerId, itemId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // --- BATTLE SYSTEM ---

    @PostMapping("/battle/start")
    public ResponseEntity<?> startBattle() {
        try {
            return ResponseEntity.ok(battleService.startBattle());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/attack")
    public ResponseEntity<?> attack(@RequestBody Map<String, Object> payload) {
        try {
            return ResponseEntity.ok(battleService.attackEnemy(payload));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/skill")
    public ResponseEntity<?> useSkill(@RequestBody Map<String, Object> payload) {
        try {
            return ResponseEntity.ok(battleService.attackEnemy(payload));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/skills")
    public ResponseEntity<?> getSkills() {
        return ResponseEntity.ok(battleService.getAllSkills());
    }
}