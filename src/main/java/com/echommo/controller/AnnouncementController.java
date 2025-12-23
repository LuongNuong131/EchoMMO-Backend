package com.echommo.controller;

import com.echommo.entity.Announcement;
import com.echommo.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    // [FIX] Thêm "/latest" để khớp với Frontend (Home.vue gọi /api/announcements/latest)
    @GetMapping("/latest")
    public ResponseEntity<List<Announcement>> getActive() {
        return ResponseEntity.ok(announcementService.getActiveAnnouncements());
    }

    // Admin: Tạo thông báo thủ công (Service sẽ check quyền Admin)
    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody Announcement announcement) {
        try {
            return ResponseEntity.ok(announcementService.createAnnouncement(announcement));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Admin: Xóa/Ẩn (Service sẽ check quyền Admin)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        try {
            announcementService.deleteAnnouncement(id);
            return ResponseEntity.ok("Đã xóa");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}