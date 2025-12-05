package com.echommo.controller;

import com.echommo.dto.ChatMessageDTO;
import com.echommo.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @GetMapping("/recent")
    public ResponseEntity<List<ChatMessageDTO>> getRecent() {
        return ResponseEntity.ok(chatService.getRecentMessages());
    }

    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody Map<String, String> payload) {
        try {
            String content = payload.get("content");
            return ResponseEntity.ok(chatService.sendMessage(content));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}