package com.echommo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // Hàm cũ của bạn (Giữ nguyên)
    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@echommo.com");
        message.setTo(toEmail);
        message.setSubject("Mã OTP lấy lại mật khẩu - EchoMMO");
        message.setText("Chào bạn,\n\nMã xác thực (OTP) của bạn là: " + otp + "\n\nMã này có hiệu lực trong 5 phút. Vui lòng không chia sẻ cho ai khác.\n\nThân ái,\nEchoMMO Team.");

        mailSender.send(message);
    }

    // --- THÊM HÀM MỚI NÀY ---
    // Hàm gửi email chung (Dùng cho Báo cáo lỗi, Thông báo, v.v.)
    public void sendEmail(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@echommo.com"); // Email gửi đi
            message.setTo(toEmail);                 // Email người nhận (Admin)
            message.setSubject(subject);            // Tiêu đề
            message.setText(body);                  // Nội dung

            mailSender.send(message);
            System.out.println("Email sent successfully to: " + toEmail);
        } catch (Exception e) {
            System.err.println("Error sending email: " + e.getMessage());
            // Có thể throw lại exception để Controller bắt được nếu cần
        }
    }
}