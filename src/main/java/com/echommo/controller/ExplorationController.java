package com.echommo.controller;

import com.echommo.dto.ExplorationResponse;
import com.echommo.service.ExplorationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/exploration")
public class ExplorationController {
    @Autowired
    private ExplorationService explorationService;

    @PostMapping("/explore")
    public ResponseEntity<?> explore() {
        try {
            ExplorationResponse result = explorationService.explore();
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // [MỚI] API Khai thác tài nguyên
    @PostMapping("/gather")
    public ResponseEntity<?> gather(@RequestBody Map<String, Object> payload) {
        try {
            String type = (String) payload.get("type");
            int amount = (int) payload.get("amount");

            Map<String, Object> result = explorationService.gatherResource(type, amount);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}