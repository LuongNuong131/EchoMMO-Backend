package com.echommo.controller;

import com.echommo.entity.User;
import com.echommo.repository.UserRepository;
import com.echommo.service.SpaService; // <--- Import file mới tạo
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/spa")
@CrossOrigin(origins = "*")
public class SpaController {

    @Autowired
    private SpaService spaService; // <--- Dùng SpaService thay vì GameService

    @Autowired
    private UserRepository userRepo;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy User!"));
    }

    @PostMapping("/use")
    public ResponseEntity<?> useSpaService(@RequestParam String packageType) {
        try {
            User user = getCurrentUser();
            // Gọi hàm processSpaTreatment từ SpaService
            Map<String, Object> result = spaService.processSpaTreatment(user.getUserId(), packageType);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}