package com.echommo.controller;

import com.echommo.entity.User;
import com.echommo.entity.UserItem; // [FIX] Import UserItem
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

    @GetMapping("/player/{id}")
    public User getPlayer(@PathVariable Integer id) {
        return gameService.getPlayerOrCreate(id);
    }

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

    @PostMapping("/explore")
    public Map<String, Object> explore(@RequestParam Integer playerId) {
        return gameService.explore(playerId);
    }

    @PostMapping("/rest")
    public User rest(@RequestParam Integer playerId) {
        return gameService.restAtInn(playerId);
    }

    // [FIX] Return List<UserItem> thay vì List<Item> để khớp với Service
    @GetMapping("/inventory/{playerId}")
    public List<UserItem> getInventory(@PathVariable Integer playerId) {
        return gameService.getInventory(playerId);
    }

    // [FIX] itemId đổi thành Long để khớp với Service
    @PostMapping("/equip")
    public Map<String, Object> equip(@RequestParam Integer playerId, @RequestParam Long itemId) {
        return gameService.equipItem(playerId, itemId);
    }

    // [FIX] itemId đổi thành Long
    @PostMapping("/unequip")
    public Map<String, Object> unequip(@RequestParam Integer playerId, @RequestParam Long itemId) {
        return gameService.unequipItem(playerId, itemId);
    }

    @PostMapping("/skill")
    public ResponseEntity<?> useSkill(@RequestBody Map<String, Object> payload) {
        try {
            return ResponseEntity.ok(battleService.attackEnemy(payload));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // [FIX] Gọi hàm getAllSkills() đã được thêm bên Service
    @GetMapping("/skills")
    public ResponseEntity<?> getSkills() {
        return ResponseEntity.ok(battleService.getAllSkills());
    }
}