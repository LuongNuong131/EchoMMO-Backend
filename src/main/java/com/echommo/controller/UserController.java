package com.echommo.controller;

import com.echommo.dto.ChangePasswordRequest;
import com.echommo.dto.UpdateProfileRequest;
import com.echommo.entity.User;
import com.echommo.repository.UserRepository;
import com.echommo.service.UserService; // Import Service mới
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService; // Inject Service

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(user);
    }

    // API Cập nhật thông tin
    @PutMapping("/update")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }

    // API Đổi mật khẩu
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            return ResponseEntity.ok(userService.changePassword(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}