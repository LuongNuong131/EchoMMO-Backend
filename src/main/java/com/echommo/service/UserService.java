package com.echommo.service;

import com.echommo.dto.AuthRequest;
import com.echommo.dto.ChangePasswordRequest;
import com.echommo.dto.UpdateProfileRequest;
import com.echommo.entity.User;
import com.echommo.entity.Wallet;
import com.echommo.enums.Role;
import com.echommo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class UserService {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private final String UPLOAD_DIR = "uploads";

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    // Helper: Lấy user hiện tại từ SecurityContext
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return findByUsername(username);
    }

    // [FIX] Hàm public để Controller gọi nếu cần check authentication thủ công
    public User getUserFromAuth(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Chưa đăng nhập!");
        }
        return findByUsername(authentication.getName());
    }

    @Transactional
    public User registerUser(AuthRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại!");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email này đã được sử dụng!");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());

        // [FIX LOGIC] Lưu cả Hash và Raw
        user.setPasswordHash(passwordEncoder.encode(req.getPassword())); // Để đăng nhập
        user.setPassword(req.getPassword()); // Để hiển thị (nếu cần)

        user.setFullName(req.getFullName());
        user.setAvatarUrl("🐲");
        user.setIsActive(true);
        user.setRole(Role.USER);

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setGold(BigDecimal.valueOf(1000));
        user.setWallet(wallet);

        return userRepository.save(user);
    }

    @Transactional
    public User updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("Tên đăng nhập đã tồn tại!");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email đã được sử dụng!");
            }
            user.setEmail(request.getEmail());
        }

        // [FIX LOGIC] Cập nhật mật khẩu trong profile
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setPassword(request.getPassword());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        // Fix logic cũ: nếu request có profileImageUrl thì set luôn (cho trường hợp upload ảnh xong gửi string về update)
        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl());
        }

        return userRepository.save(user);
    }

    @Transactional
    public User uploadAvatar(MultipartFile file) {
        User user = getCurrentUser();

        if (file.isEmpty()) {
            throw new RuntimeException("File không được để trống");
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFileName = UUID.randomUUID().toString() + fileExtension;

            Path filePath = uploadPath.resolve(newFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Lưu đường dẫn file vào DB
            String dbFilePath = "/uploads/" + newFileName;
            user.setProfileImageUrl(dbFilePath);

            return userRepository.save(user);

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi hệ thống khi lưu file: " + e.getMessage());
        }
    }

    @Transactional
    public String changePassword(ChangePasswordRequest request) {
        User user = getCurrentUser();

        // [QUAN TRỌNG] Kiểm tra mật khẩu cũ phải đối chiếu với password_hash
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng!");
        }

        // [FIX LOGIC] Lưu mật khẩu mới vào cả 2 cột
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPassword(request.getNewPassword());

        userRepository.save(user);
        return "Đổi mật khẩu thành công!";
    }
}