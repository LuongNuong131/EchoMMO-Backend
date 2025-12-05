package com.echommo.controller;

import com.echommo.entity.Item;
import com.echommo.entity.MarketListing;
import com.echommo.entity.User;
import com.echommo.enums.Role;
import com.echommo.repository.UserRepository;
import com.echommo.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
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
    @PostMapping("/user/toggle/{id}") public ResponseEntity<?> toggle(@PathVariable Integer id) { check(); s.toggleUser(id); return ResponseEntity.ok().build(); }
    @DeleteMapping("/user/{id}") public ResponseEntity<?> delUser(@PathVariable Integer id) { check(); s.deleteUser(id); return ResponseEntity.ok().build(); }

    @GetMapping("/items") public ResponseEntity<?> items() { check(); return ResponseEntity.ok(s.getAllItems()); }
    @PostMapping("/item/create") public ResponseEntity<?> createI(@RequestBody Item i) { check(); return ResponseEntity.ok(s.createItem(i)); }
    @DeleteMapping("/item/{id}") public ResponseEntity<?> delItem(@PathVariable Integer id) { check(); s.deleteItem(id); return ResponseEntity.ok().build(); }

    @GetMapping("/listings") public ResponseEntity<?> listings() { check(); return ResponseEntity.ok(s.getAllListings()); }
    @DeleteMapping("/listing/{id}") public ResponseEntity<?> delListing(@PathVariable Integer id) { check(); s.deleteListing(id); return ResponseEntity.ok().build(); }

    @PostMapping("/grant-gold") public ResponseEntity<?> gold(@RequestBody Map<String,Object> b) { check(); return ResponseEntity.ok(s.grantGold((String)b.get("username"), new BigDecimal(b.get("amount").toString()))); }
    @PostMapping("/grant-item") public ResponseEntity<?> item(@RequestBody Map<String,Object> b) { check(); return ResponseEntity.ok(s.grantItem((String)b.get("username"), (Integer)b.get("itemId"), (Integer)b.get("quantity"))); }

    // --- API GỬI THÔNG BÁO (MỚI) ---
    @PostMapping("/notification/create")
    public ResponseEntity<?> sendNoti(@RequestBody Map<String, String> payload) {
        try {
            check();
            s.sendCustomNotification(payload);
            return ResponseEntity.ok("Đã gửi thông báo");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}