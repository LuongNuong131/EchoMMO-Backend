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

    // Public: Lấy thông báo active
    @GetMapping
    public ResponseEntity<List<Announcement>> getActive() {
        return ResponseEntity.ok(announcementService.getActiveAnnouncements());
    }

    // Admin: Tạo thông báo
    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody Announcement announcement) {
        try {
            return ResponseEntity.ok(announcementService.createAnnouncement(announcement));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Admin: Xóa/Ẩn
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