package com.echommo.controller;

import com.echommo.service.PrivateMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/dm")
public class PrivateMessageController {
    @Autowired private PrivateMessageService pmService;

    @GetMapping("/{friendId}")
    public ResponseEntity<?> getConversation(@PathVariable Integer friendId) {
        return ResponseEntity.ok(pmService.getConversation(friendId));
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendDM(@RequestBody Map<String, Object> payload) {
        Integer friendId = (Integer) payload.get("friendId");
        String content = (String) payload.get("content");
        return ResponseEntity.ok(pmService.sendPrivateMessage(friendId, content));
    }
}