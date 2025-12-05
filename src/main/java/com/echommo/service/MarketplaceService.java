package com.echommo.service;

import com.echommo.dto.CreateListingRequest;
import com.echommo.entity.*;
import com.echommo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class MarketplaceService {

    @Autowired private ItemRepository itemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private UserItemRepository userItemRepository;
    @Autowired private MarketListingRepository listingRepository;
    @Autowired private NotificationService notificationService;

    // --- GET DATA ---
    public List<Item> getShopItems() { return itemRepository.findAll(); }
    public List<MarketListing> getPlayerListings() { return listingRepository.findByStatusOrderByCreatedAtDesc("ACTIVE"); }
    public List<MarketListing> getMyListings() {
        String u = SecurityContextHolder.getContext().getAuthentication().getName();
        return listingRepository.findBySeller_UserIdAndStatus(userRepository.findByUsername(u).get().getUserId(), "ACTIVE");
    }

    private User getCurrentUser() {
        String u = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(u).orElseThrow();
    }

    // --- MUA SHOP HỆ THỐNG ---
    @Transactional
    public String buyItem(Integer itemId, Integer qty) {
        if(qty <= 0) throw new RuntimeException("SL > 0");
        User user = getCurrentUser();
        Item item = itemRepository.findById(itemId).orElseThrow(()->new RuntimeException("Item 404"));

        BigDecimal cost = item.getBasePrice().multiply(new BigDecimal(qty));
        if(user.getWallet().getGold().compareTo(cost) < 0) throw new RuntimeException("Thiếu vàng");

        user.getWallet().setGold(user.getWallet().getGold().subtract(cost));
        walletRepository.save(user.getWallet());

        deliverItem(user, item, qty, 0);
        return "Mua thành công!";
    }

    // --- BÁN NPC (Lựa chọn 1) ---
    @Transactional
    public String sellItem(Integer userItemId, Integer qty) {
        if(qty <= 0) throw new RuntimeException("SL > 0");
        User user = getCurrentUser();
        UserItem ui = userItemRepository.findById(userItemId).orElseThrow();
        if(!ui.getUser().getUserId().equals(user.getUserId())) throw new RuntimeException("Lỗi quyền");
        if(ui.getIsEquipped()) throw new RuntimeException("Đang mặc");
        if(ui.getQuantity() < qty) throw new RuntimeException("Thiếu hàng");

        // Giá = 50% giá gốc
        BigDecimal earn = ui.getItem().getBasePrice().multiply(new BigDecimal("0.5")).multiply(new BigDecimal(qty));
        user.getWallet().setGold(user.getWallet().getGold().add(earn));
        walletRepository.save(user.getWallet());

        removeItem(ui, qty);
        return "Bán cho NPC thành công! Nhận " + earn + " Vàng.";
    }

    // --- ĐĂNG BÁN P2P (Lựa chọn 2) ---
    @Transactional
    public String createListing(CreateListingRequest req) {
        if(req.getQuantity() <= 0) throw new RuntimeException("SL > 0");
        User user = getCurrentUser();
        UserItem ui = userItemRepository.findById(req.getUserItemId()).orElseThrow();

        if(!ui.getUser().getUserId().equals(user.getUserId())) throw new RuntimeException("Lỗi quyền");
        if(ui.getIsEquipped()) throw new RuntimeException("Đang mặc");
        if(ui.getQuantity() < req.getQuantity()) throw new RuntimeException("Thiếu hàng");

        MarketListing ml = new MarketListing();
        ml.setSeller(user);
        ml.setItem(ui.getItem());
        ml.setQuantity(req.getQuantity());
        ml.setPrice(req.getPrice());
        ml.setEnhanceLevel(ui.getEnhanceLevel());
        ml.setStatus("ACTIVE");
        listingRepository.save(ml);

        removeItem(ui, req.getQuantity());
        return "Đã đăng bán lên chợ!";
    }

    // --- MUA P2P ---
    @Transactional
    public String buyPlayerListing(Integer listingId, Integer qtyToBuy) {
        if(qtyToBuy <= 0) throw new RuntimeException("SL > 0");
        User buyer = getCurrentUser();
        MarketListing l = listingRepository.findById(listingId).orElseThrow();

        if(!"ACTIVE".equals(l.getStatus())) throw new RuntimeException("Tin không khả dụng");
        if(l.getSeller().getUserId().equals(buyer.getUserId())) throw new RuntimeException("Không thể tự mua");
        if(l.getQuantity() < qtyToBuy) throw new RuntimeException("Không đủ hàng");

        BigDecimal total = l.getPrice().multiply(new BigDecimal(qtyToBuy));
        if(buyer.getWallet().getGold().compareTo(total) < 0) throw new RuntimeException("Thiếu vàng");

        // Trừ tiền mua
        buyer.getWallet().setGold(buyer.getWallet().getGold().subtract(total));
        walletRepository.save(buyer.getWallet());

        // Cộng tiền bán (Phí 5%)
        BigDecimal fee = total.multiply(new BigDecimal("0.05"));
        User seller = l.getSeller();
        seller.getWallet().setGold(seller.getWallet().getGold().add(total.subtract(fee)));
        walletRepository.save(seller.getWallet());

        // Giao hàng
        deliverItem(buyer, l.getItem(), qtyToBuy, l.getEnhanceLevel());

        // Cập nhật tin
        int left = l.getQuantity() - qtyToBuy;
        if(left <= 0) { l.setQuantity(0); l.setStatus("SOLD"); }
        else l.setQuantity(left);
        listingRepository.save(l);

        notificationService.sendNotification(seller, "💰 Hàng đã bán", "Bạn đã bán "+qtyToBuy+" x "+l.getItem().getName(), "SUCCESS");
        return "Mua thành công!";
    }

    // --- HỦY BÁN ---
    @Transactional
    public String cancelListing(Integer id) {
        User user = getCurrentUser();
        MarketListing l = listingRepository.findById(id).orElseThrow();
        if(!l.getSeller().getUserId().equals(user.getUserId())) throw new RuntimeException("Lỗi quyền");
        if(!"ACTIVE".equals(l.getStatus())) throw new RuntimeException("Tin lỗi");

        l.setStatus("CANCELLED");
        listingRepository.save(l);
        deliverItem(user, l.getItem(), l.getQuantity(), l.getEnhanceLevel());
        return "Đã hủy bán, trả đồ về kho.";
    }

    // Helpers
    private void removeItem(UserItem ui, int qty) {
        if(ui.getQuantity() <= qty) userItemRepository.delete(ui);
        else { ui.setQuantity(ui.getQuantity() - qty); userItemRepository.save(ui); }
    }

    private String deliverItem(User user, Item item, int qty, int enhance) {
        if("MATERIAL".equals(item.getType())) {
            Wallet w = user.getWallet();
            if(item.getName().contains("Gỗ")) w.setWood(w.getWood()+qty);
            else if(item.getName().contains("Đá")) w.setStone(w.getStone()+qty);
            else return addItem(user, item, qty, enhance);
            walletRepository.save(w);
            return "Nhận NL";
        }
        return addItem(user, item, qty, enhance);
    }

    private String addItem(User user, Item item, int qty, int enhance) {
        List<UserItem> list = userItemRepository.findByUser_UserIdOrderByIsEquippedDesc(user.getUserId());
        Optional<UserItem> ex = list.stream().filter(i->i.getItem().getItemId().equals(item.getItemId()) && i.getEnhanceLevel()==enhance && !i.getIsEquipped()).findFirst();
        if(ex.isPresent()) {
            ex.get().setQuantity(ex.get().getQuantity()+qty);
            userItemRepository.save(ex.get());
        } else {
            UserItem ui = new UserItem();
            ui.setUser(user); ui.setItem(item); ui.setQuantity(qty); ui.setEnhanceLevel(enhance); ui.setIsEquipped(false);
            userItemRepository.save(ui);
        }
        return "Nhận đồ";
    }
}