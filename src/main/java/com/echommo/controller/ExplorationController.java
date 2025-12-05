package com.echommo.controller;

import com.echommo.dto.ExplorationResponse;
import com.echommo.service.ExplorationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}