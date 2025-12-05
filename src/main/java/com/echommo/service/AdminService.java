package com.echommo.service;

import com.echommo.entity.*;
import com.echommo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
public class AdminService {
    @Autowired private UserRepository userRepo;
    @Autowired private ItemRepository itemRepo;
    @Autowired private WalletRepository walletRepo;
    @Autowired private UserItemRepository uiRepo;
    @Autowired private MarketListingRepository listingRepo;
    @Autowired private NotificationService notiService;

    // --- STATS ---
    public Map<String, Object> getServerStats() {
        Map<String, Object> m = new HashMap<>();
        m.put("totalUsers", userRepo.count());
        m.put("totalItems", itemRepo.count());
        m.put("totalGold", walletRepo.findAll().stream().map(Wallet::getGold).reduce(BigDecimal.ZERO, BigDecimal::add));
        return m;
    }

    // --- CRUD ---
    public List<Item> getAllItems() { return itemRepo.findAll(); }
    public Item createItem(Item i) {
        if(i.getImageUrl()==null || i.getImageUrl().isEmpty()) i.setImageUrl("/assets/items/default.png");
        return itemRepo.save(i);
    }
    public void deleteItem(Integer id) { itemRepo.deleteById(id); }

    public List<User> getAllUsers() { return userRepo.findAll(); }
    public void toggleUser(Integer id) { User u=userRepo.findById(id).orElseThrow(); u.setIsActive(!u.getIsActive()); userRepo.save(u); }
    public void deleteUser(Integer id) { userRepo.deleteById(id); }

    public List<MarketListing> getAllListings() { return listingRepo.findAll(); }
    public void deleteListing(Integer id) {
        MarketListing l = listingRepo.findById(id).orElseThrow();
        if("ACTIVE".equals(l.getStatus())) {
            UserItem ui = new UserItem();
            ui.setUser(l.getSeller()); ui.setItem(l.getItem()); ui.setQuantity(l.getQuantity());
            ui.setEnhanceLevel(l.getEnhanceLevel()); ui.setIsEquipped(false);
            uiRepo.save(ui);
            notiService.sendNotification(l.getSeller(), "⚠️ Tin bị gỡ", "Admin đã gỡ tin bán "+l.getItem().getName(), "WARNING");
        }
        listingRepo.deleteById(id);
    }

    // --- GRANT ---
    @Transactional
    public String grantGold(String uName, BigDecimal amt) {
        User u = userRepo.findByUsername(uName).orElseThrow();
        u.getWallet().setGold(u.getWallet().getGold().add(amt));
        walletRepo.save(u.getWallet());
        notiService.sendNotification(u, "🎁 Quà Admin", "Nhận "+amt+" Vàng", "SUCCESS");
        return "Done";
    }
    @Transactional
    public String grantItem(String uName, Integer iId, Integer qty) {
        User u = userRepo.findByUsername(uName).orElseThrow();
        Item i = itemRepo.findById(iId).orElseThrow();
        UserItem ui = new UserItem();
        ui.setUser(u); ui.setItem(i); ui.setQuantity(qty); ui.setIsEquipped(false); ui.setEnhanceLevel(0);
        uiRepo.save(ui);
        notiService.sendNotification(u, "🎁 Quà Admin", "Nhận "+qty+" x "+i.getName(), "SUCCESS");
        return "Done";
    }

    // --- NOTIFICATION (MỚI) ---
    @Transactional
    public void sendCustomNotification(Map<String, String> payload) {
        String title = payload.get("title");
        String message = payload.get("message");
        String type = payload.get("type");
        String recipient = payload.get("recipientUsername");

        if (recipient != null && !recipient.trim().isEmpty()) {
            // Gửi cho 1 người
            User user = userRepo.findByUsername(recipient)
                    .orElseThrow(() -> new RuntimeException("User không tồn tại: " + recipient));
            notiService.sendNotification(user, title, message, type);
        } else {
            // Gửi toàn Server
            List<User> allUsers = userRepo.findAll();
            for (User user : allUsers) {
                notiService.sendNotification(user, title, message, type);
            }
        }
    }
}