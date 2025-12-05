package com.echommo.controller;

import com.echommo.entity.User;
import com.echommo.repository.UserRepository;
import com.echommo.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/auth")
public class ForgotPasswordController {

    @Autowired private UserRepository userRepository;
    @Autowired private EmailService emailService;
    @Autowired private PasswordEncoder passwordEncoder;

    // 1. Gửi OTP
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        User user = userRepository.findByEmail(email).orElse(null); // Cần thêm hàm findByEmail vào Repo nếu chưa có

        if (user == null) {
            return ResponseEntity.badRequest().body("Email không tồn tại trong hệ thống.");
        }

        // Tạo OTP 6 số
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Lưu OTP và thời hạn (5 phút)
        user.setOtpCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        // Gửi mail
        try {
            emailService.sendOtpEmail(email, otp);
            return ResponseEntity.ok("Đã gửi mã OTP qua email.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi gửi mail: " + e.getMessage());
        }
    }

    // 2. Xác thực OTP và Đổi pass
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String otp = payload.get("otp");
        String newPassword = payload.get("newPassword");

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("User không tồn tại");

        if (user.getOtpCode() == null || !user.getOtpCode().equals(otp)) {
            return ResponseEntity.badRequest().body("Mã OTP không đúng.");
        }

        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Mã OTP đã hết hạn.");
        }

        // Đổi mật khẩu
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setOtpCode(null); // Xóa OTP sau khi dùng
        user.setOtpExpiry(null);
        userRepository.save(user);

        return ResponseEntity.ok("Đổi mật khẩu thành công! Hãy đăng nhập lại.");
    }
}