package com.echommo.controller;

import com.echommo.entity.UserItem;
import com.echommo.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired private InventoryService inventoryService;

    @GetMapping("/me")
    public ResponseEntity<List<UserItem>> getMyInventory() {
        return ResponseEntity.ok(inventoryService.getMyInventory());
    }

    @PostMapping("/equip/{id}")
    public ResponseEntity<?> equipItem(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(inventoryService.equipItem(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/unequip/{id}")
    public ResponseEntity<?> unequipItem(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(inventoryService.unequipItem(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/enhance/{id}")
    public ResponseEntity<?> enhanceItem(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(inventoryService.enhanceItem(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/use/{id}") // <--- API Mới
    public ResponseEntity<?> useItem(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(inventoryService.useItem(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}