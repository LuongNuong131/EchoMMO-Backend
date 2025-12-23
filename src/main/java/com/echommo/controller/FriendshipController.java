package com.echommo.controller;

import com.echommo.entity.Friendship;
import com.echommo.service.FriendshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
public class FriendshipController {

    @Autowired private FriendshipService friendshipService;

    @GetMapping("/list")
    public ResponseEntity<List<Friendship>> getFriends() {
        return ResponseEntity.ok(friendshipService.getFriendList());
    }

    @GetMapping("/requests")
    public ResponseEntity<List<Friendship>> getRequests() {
        return ResponseEntity.ok(friendshipService.getPendingRequests());
    }

    @PostMapping("/add")
    public ResponseEntity<?> sendRequest(@RequestBody Map<String, String> payload) {
        try {
            return ResponseEntity.ok(friendshipService.sendRequest(payload.get("username")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/accept/{id}")
    public ResponseEntity<?> accept(@PathVariable Integer id) {
        return ResponseEntity.ok(friendshipService.acceptRequest(id));
    }

    @DeleteMapping("/remove/{id}")
    public ResponseEntity<?> remove(@PathVariable Integer id) {
        return ResponseEntity.ok(friendshipService.removeFriend(id));
    }
}