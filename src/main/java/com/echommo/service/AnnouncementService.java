package com.echommo.service;

import com.echommo.entity.Announcement;
import com.echommo.entity.User;
import com.echommo.enums.NotificationType;
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
    @Autowired private NotificationService notificationService;

    // 1. Lấy danh sách để chạy chữ ngoài Frontend
    public List<Announcement> getActiveAnnouncements() {
        return announcementRepository.findTop10ByIsActiveTrueOrderByCreatedAtDesc();
    }

    // 2. [DÀNH CHO ADMIN] Gọi từ AdminController (Có check quyền)
    @Transactional
    public Announcement createAnnouncement(Announcement req) {
        checkAdmin(); // Chỉ Admin mới được dùng hàm này
        return saveAndBroadcast(req.getTitle(), req.getContent(), req.getType());
    }

    // 3. [DÀNH CHO HỆ THỐNG] Gọi từ InventoryService, PvpService... (KHÔNG check quyền)
    @Transactional
    public void createAnnouncement(String title, String content, String type) {
        saveAndBroadcast(title, content, type);
    }

    // 4. Logic chung: Lưu DB + Bắn thông báo toàn server
    private Announcement saveAndBroadcast(String title, String content, String type) {
        Announcement a = new Announcement();
        a.setTitle(title);
        a.setContent(content);
        a.setType(type);
        a.setIsActive(true);

        Announcement saved = announcementRepository.save(a);

        // === GỬI THÔNG BÁO CHO TOÀN BỘ USER ===
        List<User> allUsers = userRepository.findAll();

        NotificationType notiType = NotificationType.INFO;
        if ("UPGRADE".equals(type) || "EVENT".equals(type)) {
            notiType = NotificationType.SUCCESS;
        } else if ("MAINTAIN".equals(type)) {
            notiType = NotificationType.WARNING;
        } else if ("REWARD".equals(type)) {
            notiType = NotificationType.REWARD;
        }

        // Lưu ý: Nếu server đông user, đoạn này nên chạy Async hoặc bắn qua WebSocket topic
        // Nhưng với quy mô hiện tại thì vòng lặp này ổn.
        for (User user : allUsers) {
            notificationService.sendNotification(
                    user,
                    "📢 " + title,
                    "Có tin mới từ hệ thống.", // Rút gọn nội dung pop-up để tránh spam dài
                    notiType
            );
        }

        return saved;
    }

    // Xóa thông báo (Chỉ Admin)
    public void deleteAnnouncement(Integer id) {
        checkAdmin();
        announcementRepository.deleteById(id);
    }

    private void checkAdmin() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(username).orElseThrow();
            if (user.getRole() != Role.ADMIN) {
                throw new RuntimeException("Bạn không có quyền Admin");
            }
        } catch (Exception e) {
            // Trường hợp gọi từ luồng hệ thống không có Auth (nếu có)
            // Nhưng ở đây ta tách hàm rồi nên an toàn.
            throw new RuntimeException("Lỗi xác thực quyền quản trị.");
        }
    }
}