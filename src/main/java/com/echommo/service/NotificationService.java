package com.echommo.service;

import com.echommo.entity.Notification;
import com.echommo.entity.User;
import com.echommo.enums.NotificationType;
import com.echommo.repository.NotificationRepository;
import com.echommo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    @Autowired private NotificationRepository notificationRepository;
    @Autowired private UserRepository userRepository;

    public List<Notification> getMyNotifications() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        return notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(user.getUserId());
    }

    public long countUnread() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        return notificationRepository.countByUser_UserIdAndIsReadFalse(user.getUserId());
    }

    public void markAsRead(Integer id) {
        Notification noti = notificationRepository.findById(id).orElseThrow();
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if(noti.getUser().getUsername().equals(username)) {
            noti.setIsRead(true);
            notificationRepository.save(noti);
        }
    }

    public void markAllAsRead() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        List<Notification> list = notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(user.getUserId());
        for (Notification n : list) {
            if(!n.getIsRead()) {
                n.setIsRead(true);
                notificationRepository.save(n);
            }
        }
    }

    // [FIX] Sử dụng Enum NotificationType
    @Transactional
    public void sendNotification(User user, String title, String message, NotificationType type) {
        Notification n = new Notification();
        n.setUser(user);
        n.setTitle(title);
        n.setMessage(message);
        n.setType(type);
        n.setIsRead(false);
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(n);
    }
}