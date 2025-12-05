package com.echommo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@echommo.com");
        message.setTo(toEmail);
        message.setSubject("Mã OTP lấy lại mật khẩu - EchoMMO");
        message.setText("Chào bạn,\n\nMã xác thực (OTP) của bạn là: " + otp + "\n\nMã này có hiệu lực trong 5 phút. Vui lòng không chia sẻ cho ai khác.\n\nThân ái,\nEchoMMO Team.");

        mailSender.send(message);
    }
}