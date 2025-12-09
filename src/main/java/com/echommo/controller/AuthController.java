package com.echommo.controller;

import com.echommo.dto.AuthRequest;
import com.echommo.dto.AuthResponse;
import com.echommo.dto.CharacterRequest;
import com.echommo.entity.User;
import com.echommo.entity.Wallet;
import com.echommo.enums.Role;
import com.echommo.repository.UserRepository;
import com.echommo.security.JwtUtils;
import com.echommo.service.CharacterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired AuthenticationManager authenticationManager;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtUtils jwtUtils;
    @Autowired CharacterService characterService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody AuthRequest loginRequest) {
        // 1. Kiểm tra User tồn tại
        User user = userRepository.findByUsername(loginRequest.getUsername()).orElse(null);

        // 2. Kiểm tra trạng thái Ban
        if (user != null && Boolean.FALSE.equals(user.getIsActive())) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "BANNED");
            response.put("message", "Tài khoản đã bị khóa!");
            response.put("reason", user.getBanReason() != null ? user.getBanReason() : "Vi phạm quy định");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        // 3. Xác thực
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateToken((UserDetails) authentication.getPrincipal());
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String roleStr = user != null ? user.getRole().name() : "USER";
        return ResponseEntity.ok(new AuthResponse(jwt, userDetails.getUsername(), roleStr));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody AuthRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest().body("Lỗi: Username đã tồn tại!");
        }
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body("Lỗi: Email đã được sử dụng!");
        }

        // 1. Tạo User mới
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPasswordHash(encoder.encode(signUpRequest.getPassword())); // Mã hóa mật khẩu
        user.setPassword(signUpRequest.getPassword()); // (Tùy chọn) Lưu password thường nếu hệ thống cũ yêu cầu
        user.setFullName(signUpRequest.getFullName());
        user.setAvatarUrl("🐲");
        user.setIsActive(true);
        user.setRole(Role.USER);

        // 2. Tạo Ví khởi đầu
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setGold(new BigDecimal("100.00"));
        user.setWallet(wallet);

        userRepository.save(user);

        // 3. Tự động tạo Nhân vật (Character)
        try {
            // Fake login để lấy context cho CharacterService
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(signUpRequest.getUsername(), signUpRequest.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            CharacterRequest charReq = new CharacterRequest();
            charReq.setName(signUpRequest.getUsername());
            characterService.createCharacter(charReq);
        } catch (Exception e) {
            return ResponseEntity.ok("Đăng ký thành công tài khoản, nhưng lỗi tạo nhân vật: " + e.getMessage());
        }

        return ResponseEntity.ok("Đăng ký thành công! Đã tạo nhân vật và tặng quà tân thủ.");
    }
}