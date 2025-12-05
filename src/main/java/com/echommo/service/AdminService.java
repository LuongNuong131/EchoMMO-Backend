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
    @Autowired private UserItemRepository uiRepo;
    @Autowired private MarketListingRepository listingRepo;
    @Autowired private NotificationService notiService;

    public Map<String, Object> getServerStats() {
        Map<String, Object> m = new HashMap<>();
        m.put("totalUsers", userRepo.count());
        m.put("totalItems", itemRepo.count());
        m.put("totalGold", walletRepo.findAll().stream().map(Wallet::getGold).reduce(BigDecimal.ZERO, BigDecimal::add));
        return m;
    }

    public List<Item> getAllItems() { return itemRepo.findAll(); }
    public List<User> getAllUsers() { return userRepo.findAll(); }
    public List<MarketListing> getAllListings() { return listingRepo.findAll(); }

    public Item createItem(Item i) {
        if(i.getImageUrl()==null || i.getImageUrl().isEmpty()) i.setImageUrl("/assets/items/default.png");
        return itemRepo.save(i);
    }

    public void deleteItem(Integer id) { itemRepo.deleteById(id); }
    public void deleteUser(Integer id) { userRepo.deleteById(id); }
    public void deleteListing(Integer id) { listingRepo.deleteById(id); }

    // [FIX] Ban User
    public void banUser(Integer id, String reason) {
        User u = userRepo.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        u.setIsActive(false);
        u.setBanReason(reason);
        u.setBannedAt(LocalDateTime.now());
        userRepo.save(u);
    }

    // [FIX] Unban User
    public void unbanUser(Integer id) {
        User u = userRepo.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        u.setIsActive(true);
        u.setBanReason(null);
        u.setBannedAt(null);
        userRepo.save(u);
    }

    // Giữ nguyên toggle cho tương thích cũ
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