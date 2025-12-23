package com.echommo.service;

import com.echommo.dto.ChatMessageDTO;
import com.echommo.entity.PrivateMessage;
import com.echommo.entity.User;
import com.echommo.enums.Role;
import com.echommo.repository.FriendshipRepository;
import com.echommo.repository.PrivateMessageRepository;
import com.echommo.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PrivateMessageService {
    @Autowired private PrivateMessageRepository pmRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FriendshipRepository friendshipRepository;

    // --- GỬI TIN NHẮN ---
    public ChatMessageDTO sendPrivateMessage(Integer senderId, Integer receiverId, String content) {
        User me = userRepository.findById(senderId).orElseThrow(() -> new RuntimeException("Sender not found"));
        User friend = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        // Admin được chat với tất cả, User thường phải là bạn bè
        boolean isAdmin = me.getRole() == Role.ADMIN;
        // Kiểm tra null safe cho list bạn bè
        boolean isFriend = friendshipRepository.findExistingFriendship(me.getUserId(), receiverId) != null
                && !friendshipRepository.findExistingFriendship(me.getUserId(), receiverId).isEmpty();

        if (!isAdmin && !isFriend) {
            throw new RuntimeException("Phải là bạn bè mới được nhắn tin!");
        }

        PrivateMessage pm = new PrivateMessage();
        pm.setSender(me);
        pm.setReceiver(friend);
        pm.setContent(content);
        pm.setSentAt(LocalDateTime.now());
        pm.setIsRead(false);

        PrivateMessage saved = pmRepository.save(pm);
        return convertToDTO(saved);
    }

    // --- LẤY LỊCH SỬ CHAT ---
    public List<ChatMessageDTO> getChatHistory(Integer userId1, Integer userId2) {
        return pmRepository.findConversation(userId1, userId2).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // --- LẤY DANH SÁCH HỘI THOẠI ---
    public List<Map<String, Object>> getRecentConversations(Integer userId) {
        List<PrivateMessage> allMsgs = pmRepository.findAllByUserId(userId);
        Map<Integer, PrivateMessage> latestMsgMap = new LinkedHashMap<>();

        for (PrivateMessage msg : allMsgs) {
            Integer partnerId = msg.getSender().getUserId().equals(userId)
                    ? msg.getReceiver().getUserId()
                    : msg.getSender().getUserId();

            if (!latestMsgMap.containsKey(partnerId)) {
                latestMsgMap.put(partnerId, msg);
            }
        }

        List<Map<String, Object>> conversations = new ArrayList<>();
        for (Map.Entry<Integer, PrivateMessage> entry : latestMsgMap.entrySet()) {
            Integer partnerId = entry.getKey();
            PrivateMessage msg = entry.getValue();

            User partner = msg.getSender().getUserId().equals(partnerId) ? msg.getSender() : msg.getReceiver();

            Map<String, Object> conv = new HashMap<>();
            conv.put("userId", partner.getUserId());
            conv.put("username", partner.getUsername());
            conv.put("avatarUrl", partner.getAvatarUrl());
            conv.put("lastMessage", (msg.getSender().getUserId().equals(userId) ? "Bạn: " : "") + msg.getContent());
            conv.put("timestamp", msg.getSentAt());

            long unread = pmRepository.countBySender_UserIdAndReceiver_UserIdAndIsReadFalse(partnerId, userId);
            conv.put("unreadCount", unread);

            conversations.add(conv);
        }
        return conversations;
    }

    public long countTotalUnread(Integer userId) {
        return pmRepository.countByReceiver_UserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Integer senderId, Integer currentUserId) {
        pmRepository.markAsRead(senderId, currentUserId);
    }

    private ChatMessageDTO convertToDTO(PrivateMessage pm) {
        return new ChatMessageDTO(
                pm.getSender().getUserId(),
                pm.getSender().getUsername(),
                pm.getSender().getAvatarUrl(),
                pm.getContent(),
                pm.getSentAt().toString(),
                pm.getSender().getRole().name()
        );
    }
}