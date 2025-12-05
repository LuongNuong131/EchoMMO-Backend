package com.echommo.controller;

import com.echommo.entity.Item;
import com.echommo.entity.Character;
import com.echommo.entity.User; // <--- FIX: Thêm import cho User Entity
import com.echommo.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "http://localhost:5173")
public class GameController {

    @Autowired private GameService gameService;

    // FIX: Đổi kiểu trả về từ Player sang User, và kiểu ID từ Long sang Integer
    @GetMapping("/player/{id}")
    public User getPlayer(@PathVariable Integer id) {
        return gameService.getPlayerOrCreate(id);
    }

    // FIX: Đổi kiểu ID từ Long sang Integer
    @PostMapping("/attack")
    public Map<String, Object> attack(
            @RequestParam Integer playerId,
            @RequestParam(defaultValue = "false") boolean isParried,
            @RequestParam(defaultValue = "normal") String attackType
    ) {
        return gameService.attackEnemy(playerId, isParried, attackType);
    }

    // FIX: Đổi kiểu ID từ Long sang Integer
    @PostMapping("/explore")
    public Map<String, Object> explore(@RequestParam Integer playerId) {
        return gameService.explore(playerId);
    }

    // FIX: Đổi kiểu trả về từ Player sang User, và kiểu ID sang Integer
    @PostMapping("/rest")
    public User rest(@RequestParam Integer playerId) {
        return gameService.restAtInn(playerId);
    }

    // FIX: Đổi kiểu ID từ Long sang Integer
    @GetMapping("/inventory/{playerId}")
    public List<Item> getInventory(@PathVariable Integer playerId) {
        return gameService.getInventory(playerId);
    }

    // FIX: Đổi kiểu ID từ Long sang Integer (cả Player ID và Item ID)
    @PostMapping("/equip")
    public Map<String, Object> equip(@RequestParam Integer playerId, @RequestParam Integer itemId) {
        return gameService.equipItem(playerId, itemId);
    }

    // FIX: Đổi kiểu ID từ Long sang Integer (cả Player ID và Item ID)
    @PostMapping("/unequip")
    public Map<String, Object> unequip(@RequestParam Integer playerId, @RequestParam Integer itemId) {
        return gameService.unequipItem(playerId, itemId);
    }
}