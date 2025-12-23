package com.echommo.controller;

import com.echommo.entity.DailyQuest;
import com.echommo.service.QuestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/quests")
public class QuestController {
    @Autowired private QuestService questService;

    @GetMapping("/daily")
    public ResponseEntity<List<DailyQuest>> getDaily() {
        return ResponseEntity.ok(questService.getDailyQuests());
    }

    @PostMapping("/claim/{id}")
    public ResponseEntity<?> claim(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(questService.claimReward(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}