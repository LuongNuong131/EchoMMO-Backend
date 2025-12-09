package com.echommo.service;

import com.echommo.entity.*;
import com.echommo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdminService {
    @Autowired private UserRepository userRepo;
    @Autowired private ItemRepository itemRepo;
    @Autowired private WalletRepository walletRepo;
    @Autowired private UserItemRepository uiRepo; // Repo quản lý túi đồ
    @Autowired private MarketListingRepository listingRepo;
    @Autowired private NotificationService notiService;

    public Map<String, Object> getServerStats() {
        Map<String, Object> m = new HashMap<>();
        m.put("totalUsers", userRepo.count());
        m.put("totalItems", itemRepo.count());
        // Tính tổng vàng an toàn hơn, tránh NullPointerException
        BigDecimal totalGold = walletRepo.findAll().stream()
                .map(Wallet::getGold)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        m.put("totalGold", totalGold);
        return m;
    }

    public List<Item> getAllItems() { return itemRepo.findAll(); }
    public List<User> getAllUsers() { return userRepo.findAll(); }
    public List<MarketListing> getAllListings() { return listingRepo.findAll(); }

    public Item createItem(Item i) {
        if(i.getImageUrl()==null || i.getImageUrl().isEmpty()) i.setImageUrl("default_item");
        return itemRepo.save(i);
    }

    public void deleteItem(Integer id) { itemRepo.deleteById(id); }

    // [FIX] Cập nhật hàm xóa User chuẩn logic
    @Transactional
    public void deleteUser(Integer id) {
        // 1. Tìm user
        User u = userRepo.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

        // 2. [FIX QUAN TRỌNG] Xóa túi đồ (UserItem) của user, KHÔNG PHẢI xóa Item gốc
        // Nếu không xóa cái này, DB sẽ báo lỗi khóa ngoại (Foreign Key Constraint)
        List<UserItem> userItems = uiRepo.findByUser_UserId(id);
        if (userItems != null && !userItems.isEmpty()) {
            uiRepo.deleteAll(userItems);
        }

        // 3. Xóa User (Cascade sẽ tự xóa Wallet, Character...)
        userRepo.delete(u);
    }

    public void deleteListing(Integer id) { listingRepo.deleteById(id); }

    public void banUser(Integer id, String reason) {
        User u = userRepo.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        u.setIsActive(false);
        u.setBanReason(reason);
        u.setBannedAt(LocalDateTime.now());
        userRepo.save(u);
    }

    public void unbanUser(Integer id) {
        User u = userRepo.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        u.setIsActive(true);
        u.setBanReason(null);
        u.setBannedAt(null);
        userRepo.save(u);
    }

    public void toggleUser(Integer id) {
        User u = userRepo.findById(id).orElseThrow();
        if(u.getIsActive()) banUser(id, "Khóa nhanh bởi Admin");
        else unbanUser(id);
    }

    @Transactional
    public String grantGold(String uName, BigDecimal amt) {
        User u = userRepo.findByUsername(uName).orElseThrow();
        u.getWallet().setGold(u.getWallet().getGold().add(amt));
        walletRepo.save(u.getWallet());
        return "Done";
    }

    @Transactional
    public String grantItem(String uName, Integer iId, Integer qty) {
        User u = userRepo.findByUsername(uName).orElseThrow();
        Item i = itemRepo.findById(iId).orElseThrow();
        UserItem ui = new UserItem();
        ui.setUser(u); ui.setItem(i); ui.setQuantity(qty); ui.setIsEquipped(false); ui.setEnhanceLevel(0);
        uiRepo.save(ui);
        return "Done";
    }

    @Transactional
    public void sendCustomNotification(Map<String, String> payload) {
        String title = payload.get("title");
        String message = payload.get("message");
        String type = payload.get("type");
        String recipient = payload.get("recipientUsername");

        if (recipient != null && !recipient.trim().isEmpty()) {
            User user = userRepo.findByUsername(recipient).orElseThrow();
            notiService.sendNotification(user, title, message, type);
        } else {
            List<User> allUsers = userRepo.findAll();
            for (User user : allUsers) {
                notiService.sendNotification(user, title, message, type);
            }
        }
    }
}