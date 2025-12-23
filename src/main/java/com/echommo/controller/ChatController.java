package com.echommo.controller;

import com.echommo.dto.ChatMessageDTO;
import com.echommo.entity.Friendship;
import com.echommo.entity.Message;
import com.echommo.entity.User;
import com.echommo.repository.FriendshipRepository;
import com.echommo.repository.MessageRepository;
import com.echommo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    // --- 1. SOCKET: Chat Public ---
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessageDTO sendMessage(@Payload ChatMessageDTO chatMessage) {
        if (chatMessage.getTimestamp() == null) {
            chatMessage.setTimestamp(LocalDateTime.now().toString());
        }
        return chatMessage;
    }

    // --- 2. SOCKET: User Join (Logic gửi riêng cho bạn bè) ---
    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessageDTO chatMessage,
                        SimpMessageHeaderAccessor headerAccessor) {

        String username = chatMessage.getSenderName();

        if (username != null) {
            headerAccessor.getSessionAttributes().put("username", username);

            User currentUser = userRepository.findByUsername(username).orElse(null);

            if (currentUser != null) {
                // Lấy danh sách bạn bè
                List<Friendship> friends = friendshipRepository.findAllFriends(currentUser.getUserId());

                // Tạo thông báo
                ChatMessageDTO notificationMsg = new ChatMessageDTO();
                notificationMsg.setSenderName("Hệ thống");
                notificationMsg.setContent(username + " đã online.");
                notificationMsg.setTimestamp(LocalDateTime.now().toString());
                notificationMsg.setRole("SYSTEM");
                notificationMsg.setAvatarUrl(currentUser.getAvatarUrl());

                // Gửi riêng
                for (Friendship f : friends) {
                    User friendUser = (f.getRequester().getUserId().equals(currentUser.getUserId()))
                            ? f.getAddressee()
                            : f.getRequester();

                    // Gửi vào hàng đợi riêng của user đó
                    messagingTemplate.convertAndSendToUser(
                            friendUser.getUsername(),
                            "/queue/notifications",
                            notificationMsg
                    );
                }
            }
        }
    }

    // --- 3. REST API: Lấy lịch sử ---
    @GetMapping("/api/chat/recent")
    public ResponseEntity<List<ChatMessageDTO>> getRecentMessages() {
        try {
            List<Message> messages = messageRepository.findTop50ByOrderBySentAtDesc();
            if (messages == null) return ResponseEntity.ok(new ArrayList<>());
            Collections.reverse(messages);
            List<ChatMessageDTO> dtos = messages.stream().map(msg -> {
                try {
                    String role = "USER";
                    String avatar = "";
                    String senderName = "Ẩn danh";
                    Integer senderId = 0;
                    if (msg.getSender() != null) {
                        senderName = msg.getSender().getUsername();
                        senderId = msg.getSender().getUserId();
                        avatar = msg.getSender().getAvatarUrl();
                        if (msg.getSender().getRole() != null) role = msg.getSender().getRole().name();
                    }
                    return new ChatMessageDTO(senderId, senderName, avatar, msg.getContent(), msg.getSentAt().toString(), role);
                } catch (Exception e) { return null; }
            }).filter(item -> item != null).collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) { return ResponseEntity.ok(new ArrayList<>()); }
    }

    // --- 4. REST API: Gửi tin nhắn (Backup) ---
    @PostMapping("/api/chat/send")
    public void sendRestMessage(@RequestBody ChatMessageDTO chatMessage) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                chatMessage.setSenderName(user.getUsername());
                chatMessage.setSenderId(user.getUserId());
                chatMessage.setAvatarUrl(user.getAvatarUrl());
                chatMessage.setRole(user.getRole() != null ? user.getRole().name() : "USER");
                try {
                    Message msgEntity = new Message();
                    msgEntity.setSender(user);
                    msgEntity.setContent(chatMessage.getContent());
                    msgEntity.setSentAt(LocalDateTime.now());
                    messageRepository.save(msgEntity);
                } catch (Exception e) { }
            }
        }
        if (chatMessage.getTimestamp() == null) chatMessage.setTimestamp(LocalDateTime.now().toString());
        messagingTemplate.convertAndSend("/topic/public", chatMessage);
    }

    // --- 5. [MỚI] API Xóa lịch sử chat (Chỉ Admin) ---
    @DeleteMapping("/api/chat/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> clearChatHistory() {
        try {
            messageRepository.deleteAll();
            return ResponseEntity.ok("Đã xóa toàn bộ lịch sử chat.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi xóa chat: " + e.getMessage());
        }
    }
}