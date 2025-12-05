package com.echommo.service;

import com.echommo.entity.Friendship;
import com.echommo.entity.User;
import com.echommo.entity.PrivateMessage; // M nhớ tạo Entity này theo SQL đã chạy
import com.echommo.repository.FriendshipRepository;
import com.echommo.repository.PrivateMessageRepository;
import com.echommo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PrivateMessageService {
    @Autowired private PrivateMessageRepository pmRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FriendshipRepository friendshipRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow();
    }

    public List<PrivateMessage> getConversation(Integer friendId) {
        User me = getCurrentUser();
        // Lấy tin nhắn 2 chiều
        return pmRepository.findConversation(me.getUserId(), friendId);
    }

    public PrivateMessage sendPrivateMessage(Integer friendId, String content) {
        User me = getCurrentUser();
        User friend = userRepository.findById(friendId).orElseThrow(() -> new RuntimeException("User not found"));

        // Check xem có phải bạn bè không
        if (friendshipRepository.findExistingFriendship(me.getUserId(), friendId).isEmpty()) {
            throw new RuntimeException("Phải là bạn bè mới được nhắn tin!");
        }

        PrivateMessage pm = new PrivateMessage();
        pm.setSender(me);
        pm.setReceiver(friend);
        pm.setContent(content);
        return pmRepository.save(pm);
    }
}