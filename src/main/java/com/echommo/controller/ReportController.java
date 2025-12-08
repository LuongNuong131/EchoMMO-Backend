package com.echommo.controller; // Đổi package cho đúng project của bạn

import com.echommo.service.EmailService;
// Tạo class DTO bên dưới hoặc import nếu đã tách file
// import com.echommo.dto.ReportRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import lombok.Data; // Nếu dùng lombok

@RestController
@RequestMapping("/api/report")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ReportController {

    @Autowired
    private EmailService emailService;

    // Thay bằng email thật của bạn (Admin nhận báo cáo)
    private static final String ADMIN_EMAIL = "echommofpoly@gmail.com";

    @PostMapping("/send")
    public ResponseEntity<?> sendReport(@RequestBody ReportRequest req) {
        // 1. Lấy tên người gửi (nếu có đăng nhập)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser"))
                ? auth.getName()
                : "Khách vãng lai (Chưa đăng nhập)";

        // 2. Soạn nội dung
        String subject = "[BUG REPORT] " + req.getTitle();
        String body = "Người báo cáo: " + username + "\n" +
                "Thời gian: " + new java.util.Date() + "\n" +
                "--------------------------------------------------\n" +
                "NỘI DUNG LỖI:\n" + req.getDescription();

        // 3. Gọi hàm mới trong Service
        emailService.sendEmail(ADMIN_EMAIL, subject, body);

        return ResponseEntity.ok().body("{\"message\": \"Đã gửi báo cáo thành công\"}");
    }

    // DTO Class (Có thể để chung file này hoặc tách ra file riêng)
    @Data
    public static class ReportRequest {
        private String title;
        private String description;
    }
}