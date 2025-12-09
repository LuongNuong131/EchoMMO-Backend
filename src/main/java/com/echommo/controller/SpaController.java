//package com.echommo.controller;
//
//import com.echommo.entity.User;
//import com.echommo.repository.UserRepository;
//import com.echommo.service.SpaService; // <--- Import file mới tạo
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/spa")
//@CrossOrigin(origins = "*")
//public class SpaController {
//
//    @Autowired
//    private SpaService spaService; // <--- Dùng SpaService thay vì GameService
//
//    @Autowired
//    private UserRepository userRepo;
//
//    private User getCurrentUser() {
//        String username = SecurityContextHolder.getContext().getAuthentication().getName();
//        return userRepo.findByUsername(username)
//                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy User!"));
//    }
//
//    @PostMapping("/use")
//    public ResponseEntity<?> useSpaService(@RequestParam String packageType) {
//        try {
//            User user = getCurrentUser();
//            // Gọi hàm processSpaTreatment từ SpaService
//            Map<String, Object> result = spaService.processSpaTreatment(user.getUserId(), packageType);
//            return ResponseEntity.ok(result);
//
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(Map.of(
//                    "success", false,
//                    "message", e.getMessage()
//            ));
//        }
//    }
//}
package com.echommo.controller;

import com.echommo.entity.User;
import com.echommo.repository.UserRepository;
import com.echommo.service.SpaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/spa")
@CrossOrigin(origins = "*") // Lưu ý: Khi deploy thật nên hạn chế origins cụ thể
public class SpaController {

    private final SpaService spaService;
    private final UserRepository userRepo;

    // [CHUẨN] Constructor Injection: Giúp code rõ ràng và an toàn hơn
    public SpaController(SpaService spaService, UserRepository userRepo) {
        this.spaService = spaService;
        this.userRepo = userRepo;
    }

    // Hàm tiện ích lấy User từ Token
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy thông tin người dùng trong hệ thống!"));
    }

    @PostMapping("/use")
    // Dùng @RequestParam là OK cho 1 tham số.
    // Nếu nhiều tham số hơn nên chuyển sang @RequestBody SpaRequestDto
    public ResponseEntity<?> useSpaService(@RequestParam String packageType) {
        try {
            User user = getCurrentUser();

            // Gọi Service xử lý logic
            Map<String, Object> result = spaService.processSpaTreatment(user.getUserId(), packageType);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            // Lỗi do input sai (gói không tồn tại, không đủ tiền...) -> Trả về 400
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            // Lỗi hệ thống không mong muốn -> Nên trả về 500
            e.printStackTrace(); // Log lỗi ra server console để debug
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Lỗi hệ thống: " + e.getMessage()
            ));
        }
    }
}