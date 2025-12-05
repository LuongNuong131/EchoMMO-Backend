package com.echommo.service;

import com.echommo.entity.Announcement;
import com.echommo.entity.User;
import com.echommo.enums.Role;
import com.echommo.repository.AnnouncementRepository;
import com.echommo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnnouncementService {

    @Autowired private AnnouncementRepository announcementRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationService notificationService; // <--- Inject thêm cái này

    public List<Announcement> getActiveAnnouncements() {
        return announcementRepository.findByIsActiveTrueOrderByCreatedAtDesc();
    }

    @Transactional
    public Announcement createAnnouncement(Announcement req) {
        checkAdmin();
        req.setIsActive(true);
        Announcement saved = announcementRepository.save(req);

        // === LOGIC MỚI: GỬI THÔNG BÁO CHO TOÀN BỘ USER ===
        // Lấy tất cả user đang hoạt động
        List<User> allUsers = userRepository.findAll(); // Hoặc lọc findByIsActiveTrue() nếu muốn

        String notiType = "INFO";
        if ("EVENT".equals(req.getType())) notiType = "SUCCESS"; // Icon quà
        else if ("MAINTAIN".equals(req.getType())) notiType = "WARNING"; // Icon cảnh báo

        for (User user : allUsers) {
            // Không cần gửi thông báo cho chính Admin vừa đăng (hoặc gửi cũng được)
            notificationService.sendNotification(
                    user,
                    "📢 " + req.getTitle(), // Thêm icon loa cho nổi
                    req.getContent(),
                    notiType
            );
        }
        // =================================================

        return saved;
    }

    public void deleteAnnouncement(Integer id) {
        checkAdmin();
        announcementRepository.deleteById(id);
    }

    private void checkAdmin() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        if (user.getRole() != Role.ADMIN) {
            throw new RuntimeException("Bạn không có quyền Admin");
        }
    }
}