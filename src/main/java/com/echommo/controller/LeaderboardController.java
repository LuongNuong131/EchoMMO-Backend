package com.echommo.controller;

import com.echommo.dto.LeaderboardEntry;
import com.echommo.service.LeaderboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    @Autowired
    private LeaderboardService leaderboardService;

    @GetMapping("/level")
    public ResponseEntity<List<LeaderboardEntry>> getLevelLeaderboard() {
        return ResponseEntity.ok(leaderboardService.getLevelLeaderboard());
    }

    @GetMapping("/wealth")
    public ResponseEntity<List<LeaderboardEntry>> getWealthLeaderboard() {
        return ResponseEntity.ok(leaderboardService.getWealthLeaderboard());
    }
}