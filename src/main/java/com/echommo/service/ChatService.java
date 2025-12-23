package com.echommo.service;

import com.echommo.dto.ChatMessageDTO;
import com.echommo.entity.Message;
import com.echommo.entity.User;
import com.echommo.repository.MessageRepository;
import com.echommo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ChatService {

    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private UserRepository userRepository;

    public List<ChatMessageDTO> getRecentMessages() {
        List<Message> messages = messageRepository.findTop50ByOrderBySentAtDesc();
        List<ChatMessageDTO> dtos = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        for (Message msg : messages) {
            // [FIXED] Thêm getUserId() và getAvatarUrl() cho đủ 6 tham số
            dtos.add(new ChatMessageDTO(
                    msg.getSender().getUserId(),       // senderId
                    msg.getSender().getUsername(),     // senderName
                    msg.getSender().getAvatarUrl(),    // avatarUrl
                    msg.getContent(),                  // content
                    msg.getSentAt().format(formatter), // timestamp
                    msg.getSender().getRole().toString() // role
            ));
        }
        // Đảo ngược lại để tin cũ ở trên, tin mới ở dưới
        Collections.reverse(dtos);
        return dtos;
    }

    public ChatMessageDTO sendMessage(String content) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("Nội dung tin nhắn trống");
        }

        Message msg = new Message();
        msg.setSender(user);
        msg.setContent(content.trim());

        messageRepository.save(msg);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        // [FIXED] Thêm getUserId() và getAvatarUrl() cho đủ 6 tham số
        return new ChatMessageDTO(
                user.getUserId(),                // senderId
                user.getUsername(),              // senderName
                user.getAvatarUrl(),             // avatarUrl
                msg.getContent(),                // content
                msg.getSentAt().format(formatter), // timestamp
                user.getRole().toString()        // role
        );
    }
}