package com.echommo.controller;

import com.echommo.entity.Item;
import com.echommo.enums.Role;
import com.echommo.repository.UserRepository;
import com.echommo.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired private AdminService s;
    @Autowired private UserRepository uRepo;

    private void check() {
        String un = SecurityContextHolder.getContext().getAuthentication().getName();
        if(uRepo.findByUsername(un).get().getRole() != Role.ADMIN) throw new RuntimeException("Denied");
    }

    @GetMapping("/stats") public ResponseEntity<?> stats() { check(); return ResponseEntity.ok(s.getServerStats()); }
    @GetMapping("/users") public ResponseEntity<?> users() { check(); return ResponseEntity.ok(s.getAllUsers()); }
    @GetMapping("/items") public ResponseEntity<?> items() { check(); return ResponseEntity.ok(s.getAllItems()); }
    @GetMapping("/listings") public ResponseEntity<?> listings() { check(); return ResponseEntity.ok(s.getAllListings()); }

    // [MỚI] API Ban User
    @PostMapping("/user/ban/{id}")
    public ResponseEntity<?> ban(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        check();
        s.banUser(id, body.get("reason"));
        return ResponseEntity.ok("Đã khóa");
    }

    // [MỚI] API Unban User
    @PostMapping("/user/unban/{id}")
    public ResponseEntity<?> unban(@PathVariable Integer id) {
        check();
        s.unbanUser(id);
        return ResponseEntity.ok("Đã mở khóa");
    }

    @PostMapping("/user/toggle/{id}") public ResponseEntity<?> toggle(@PathVariable Integer id) { check(); s.toggleUser(id); return ResponseEntity.ok().build(); }
    @DeleteMapping("/user/{id}") public ResponseEntity<?> delUser(@PathVariable Integer id) { check(); s.deleteUser(id); return ResponseEntity.ok().build(); }

    @PostMapping("/item/create") public ResponseEntity<?> createI(@RequestBody Item i) { check(); return ResponseEntity.ok(s.createItem(i)); }
    @DeleteMapping("/item/{id}") public ResponseEntity<?> delItem(@PathVariable Integer id) { check(); s.deleteItem(id); return ResponseEntity.ok().build(); }
    @DeleteMapping("/listing/{id}") public ResponseEntity<?> delList(@PathVariable Integer id) { check(); s.deleteListing(id); return ResponseEntity.ok().build(); }

    @PostMapping("/grant-gold") public ResponseEntity<?> gold(@RequestBody Map<String,Object> b) { check(); return ResponseEntity.ok(s.grantGold((String)b.get("username"), new BigDecimal(b.get("amount").toString()))); }
    @PostMapping("/grant-item") public ResponseEntity<?> item(@RequestBody Map<String,Object> b) { check(); return ResponseEntity.ok(s.grantItem((String)b.get("username"), (Integer)b.get("itemId"), (Integer)b.get("quantity"))); }

    @PostMapping("/notification/create")
    public ResponseEntity<?> sendNoti(@RequestBody Map<String, String> payload) {
        check();
        s.sendCustomNotification(payload);
        return ResponseEntity.ok("Đã gửi");
    }
}