package com.echommo.controller;

import com.echommo.dto.AuthRequest;
import com.echommo.dto.AuthResponse;
import com.echommo.dto.CharacterRequest;
import com.echommo.entity.User;
import com.echommo.entity.Wallet;
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
    @Autowired CharacterService characterService; // <--- Inject thêm

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody AuthRequest loginRequest) {
        // 1. Check User tồn tại và trạng thái Ban trước khi xác thực
        User user = userRepository.findByUsername(loginRequest.getUsername()).orElse(null);
        if (user != null && !user.getIsActive()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "BANNED");
            response.put("message", "Tài khoản đã bị khóa!");
            response.put("reason", user.getBanReason() != null ? user.getBanReason() : "Vi phạm quy định");
            response.put("date", user.getBannedAt() != null ? user.getBannedAt().toString() : "N/A");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateToken((UserDetails) authentication.getPrincipal());
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(new AuthResponse(jwt, userDetails.getUsername(), "USER"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody AuthRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest().body("Lỗi: Username đã tồn tại!");
        }
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body("Lỗi: Email đã được sử dụng!");
        }

        // 1. Tạo User
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPasswordHash(encoder.encode(signUpRequest.getPassword()));
        user.setFullName(signUpRequest.getFullName());
        user.setAvatarUrl("🐲"); // Avatar mặc định

        // 2. Tạo Ví
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setGold(new BigDecimal("100.00")); // Tặng 100 vàng
        user.setWallet(wallet);

        userRepository.save(user);

        // 3. AUTO-CREATE CHARACTER (Fix lỗi #1 & #5)
        // Login tạm thời để có Context cho CharacterService (hoặc sửa Service để nhận User)
        // Cách nhanh nhất: Mock context hoặc sửa CharacterService.createCharacter
        // Ở đây ta sẽ login thủ công luôn để CharacterService lấy được current user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(signUpRequest.getUsername(), signUpRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            CharacterRequest charReq = new CharacterRequest();
            charReq.setName(signUpRequest.getUsername()); // Tên nhân vật = Username mặc định
            characterService.createCharacter(charReq); // Hàm này đã có logic tặng đồ tân thủ
        } catch (Exception e) {
            return ResponseEntity.ok("Đăng ký thành công nhưng lỗi tạo nhân vật: " + e.getMessage());
        }

        return ResponseEntity.ok("Đăng ký thành công! Đã tạo nhân vật và tặng quà tân thủ.");
    }
}