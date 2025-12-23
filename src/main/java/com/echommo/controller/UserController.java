package com.echommo.controller;

import com.echommo.dto.ChangePasswordRequest;
import com.echommo.dto.UpdateProfileRequest;
import com.echommo.entity.User;
import com.echommo.repository.UserRepository;
import com.echommo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*") // Cẩn thận với CORS production, nhưng dev thì ok
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    // --- 1. Lấy thông tin cá nhân (Kèm Vàng/Ngọc) ---
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            // Dùng hàm repository custom để fetch luôn wallet nếu cần,
            // hoặc dùng userService.findByUsername(username) nếu Lazy Load đã được xử lý
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Đảm bảo password không bị trả về client (dù đã có @JsonIgnore nhưng cẩn tắc vô áy náy)
            user.setPassword(null);
            user.setPasswordHash(null);

            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Unauthorized: " + e.getMessage());
        }
    }

    // --- 2. Cập nhật thông tin text (Tên, Email, Password raw...) ---
    @PutMapping("/update")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest request) {
        try {
            return ResponseEntity.ok(userService.updateProfile(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- 2.1 API Upload Avatar (File) ---
    @PostMapping("/upload-avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(userService.uploadAvatar(file));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi upload: " + e.getMessage());
        }
    }

    // --- 3. Đổi mật khẩu (Chuyên biệt) ---
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            return ResponseEntity.ok(userService.changePassword(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}