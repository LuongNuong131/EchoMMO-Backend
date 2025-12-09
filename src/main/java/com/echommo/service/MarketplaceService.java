package com.echommo.service;

import com.echommo.dto.CreateListingRequest;
import com.echommo.entity.*;
import com.echommo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    public List<MarketListing> getPlayerListings() {
        return listingRepository.findByStatusOrderByCreatedAtDesc("ACTIVE");
    }

    public List<MarketListing> getMyListings() {
        User user = getCurrentUser();
        return listingRepository.findBySeller_UserIdAndStatus(user.getUserId(), "ACTIVE");
    }

    private User getCurrentUser() {
        String u = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(u)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // --- MUA SHOP HỆ THỐNG ---
    @Transactional
    public String buyItem(Integer itemId, Integer qty) {
        if (qty <= 0) throw new RuntimeException("Số lượng phải > 0");
        User user = getCurrentUser();

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Vật phẩm không tồn tại"));

        BigDecimal price = BigDecimal.valueOf(item.getBasePrice());
        BigDecimal cost = price.multiply(BigDecimal.valueOf(qty));

        if (user.getWallet().getGold().compareTo(cost) < 0)
            throw new RuntimeException("Bạn không đủ vàng!");

        user.getWallet().setGold(user.getWallet().getGold().subtract(cost));
        walletRepository.save(user.getWallet());

        deliverItem(user, item, qty, 0);
        return "Mua thành công!";
    }

    // --- BÁN CHO NPC (UserItem ID là Long) ---
    @Transactional
    public String sellItem(Long userItemId, Integer qty) {
        if (qty <= 0) throw new RuntimeException("Số lượng phải > 0");
        User user = getCurrentUser();

        UserItem ui = userItemRepository.findById(userItemId)
                .orElseThrow(() -> new RuntimeException("Vật phẩm không tồn tại"));

        if (!ui.getUser().getUserId().equals(user.getUserId()))
            throw new RuntimeException("Vật phẩm không phải của bạn");
        if (Boolean.TRUE.equals(ui.getIsEquipped()))
            throw new RuntimeException("Không thể bán vật phẩm đang mặc");
        if (ui.getQuantity() < qty)
            throw new RuntimeException("Không đủ số lượng để bán");

        BigDecimal basePrice = BigDecimal.valueOf(ui.getItem().getBasePrice());
        BigDecimal earn = basePrice
                .multiply(new BigDecimal("0.5"))
                .multiply(BigDecimal.valueOf(qty));

        user.getWallet().setGold(user.getWallet().getGold().add(earn));
        walletRepository.save(user.getWallet());

        removeItem(ui, qty);
        return "Bán thành công! Nhận được " + earn + " Vàng.";
    }

    // --- ĐĂNG BÁN CHỢ (UserItem ID là Long) ---
    @Transactional
    public String createListing(CreateListingRequest req) {
        if (req.getQuantity() <= 0) throw new RuntimeException("Số lượng phải > 0");
        User user = getCurrentUser();

        Long uItemId = req.getUserItemId();
        UserItem ui = userItemRepository.findById(uItemId)
                .orElseThrow(() -> new RuntimeException("Vật phẩm không tồn tại"));

        if (!ui.getUser().getUserId().equals(user.getUserId()))
            throw new RuntimeException("Lỗi quyền sở hữu");
        if (Boolean.TRUE.equals(ui.getIsEquipped()))
            throw new RuntimeException("Đang trang bị, vui lòng tháo ra trước khi bán");
        if (ui.getQuantity() < req.getQuantity())
            throw new RuntimeException("Không đủ số lượng");

        MarketListing ml = new MarketListing();
        ml.setSeller(user);
        ml.setItem(ui.getItem());
        ml.setQuantity(req.getQuantity());
        ml.setPrice(req.getPrice());
        // [FIX] Phòng hờ null ngay lúc tạo
        ml.setEnhanceLevel(ui.getEnhanceLevel() != null ? ui.getEnhanceLevel() : 0);
        ml.setStatus("ACTIVE");
        listingRepository.save(ml);

        removeItem(ui, req.getQuantity());
        return "Đã đăng bán lên chợ thành công!";
    }

    // --- MUA TỪ CHỢ (Listing ID là Integer) ---
    @Transactional
    public String buyPlayerListing(Integer listingId, Integer qtyToBuy) {
        if (qtyToBuy <= 0) throw new RuntimeException("SL > 0");
        User buyer = getCurrentUser();

        MarketListing l = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Tin đăng không tồn tại"));

        if (!"ACTIVE".equals(l.getStatus()))
            throw new RuntimeException("Vật phẩm này không còn bán");
        if (l.getSeller().getUserId().equals(buyer.getUserId()))
            throw new RuntimeException("Không thể tự mua đồ của mình");
        if (l.getQuantity() < qtyToBuy)
            throw new RuntimeException("Số lượng còn lại không đủ");

        BigDecimal total = l.getPrice().multiply(BigDecimal.valueOf(qtyToBuy));
        if (buyer.getWallet().getGold().compareTo(total) < 0)
            throw new RuntimeException("Bạn không đủ vàng");

        // 1. Trừ tiền người mua
        buyer.getWallet().setGold(buyer.getWallet().getGold().subtract(total));
        walletRepository.save(buyer.getWallet());

        // 2. Cộng tiền người bán (Trừ phí 5% - Có làm tròn)
        BigDecimal fee = total.multiply(new BigDecimal("0.05"));
        BigDecimal sellerReceive = total.subtract(fee).setScale(2, RoundingMode.HALF_UP);

        User seller = l.getSeller();
        seller.getWallet().setGold(seller.getWallet().getGold().add(sellerReceive));
        walletRepository.save(seller.getWallet());

        // 3. Giao hàng - [FIX QUAN TRỌNG] Handle null enhance level
        int enhanceLvl = l.getEnhanceLevel() != null ? l.getEnhanceLevel() : 0;
        deliverItem(buyer, l.getItem(), qtyToBuy, enhanceLvl);

        // 4. Cập nhật tin đăng
        int left = l.getQuantity() - qtyToBuy;
        if (left <= 0) {
            l.setQuantity(0);
            l.setStatus("SOLD");
        } else {
            l.setQuantity(left);
        }
        listingRepository.save(l);

        notificationService.sendNotification(seller,
                "💰 Hàng đã bán",
                "Bạn đã bán " + qtyToBuy + " x " + l.getItem().getName() + ". Nhận được: " + sellerReceive + " vàng",
                "SUCCESS");

        return "Mua thành công!";
    }

    // --- HỦY BÁN (Listing ID là Integer) ---
    @Transactional
    public String cancelListing(Integer id) {
        User user = getCurrentUser();
        MarketListing l = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tin đăng không tồn tại"));

        if (!l.getSeller().getUserId().equals(user.getUserId()))
            throw new RuntimeException("Không phải tin đăng của bạn");
        if (!"ACTIVE".equals(l.getStatus()))
            throw new RuntimeException("Tin đăng không thể hủy");

        l.setStatus("CANCELLED");
        listingRepository.save(l);

        // [FIX QUAN TRỌNG] Handle null enhance level
        int enhanceLvl = l.getEnhanceLevel() != null ? l.getEnhanceLevel() : 0;
        deliverItem(user, l.getItem(), l.getQuantity(), enhanceLvl);
        return "Đã hủy bán, vật phẩm đã trở về kho.";
    }

    // --- HELPERS ---
    private void removeItem(UserItem ui, int qty) {
        if (ui.getQuantity() <= qty) {
            userItemRepository.delete(ui);
        } else {
            ui.setQuantity(ui.getQuantity() - qty);
            userItemRepository.save(ui);
        }
    }

    private String deliverItem(User user, Item item, int qty, int enhance) {
        return addItem(user, item, qty, enhance);
    }

    private String addItem(User user, Item item, int qty, int enhance) {
        List<UserItem> list = userItemRepository.findByUser_UserIdOrderByIsEquippedDesc(user.getUserId());

        Optional<UserItem> ex = list.stream()
                .filter(i -> i.getItem().getItemId().equals(item.getItemId())
                        && i.getEnhanceLevel().equals(enhance) // Dùng equals cho Integer
                        && !Boolean.TRUE.equals(i.getIsEquipped()))
                .findFirst();

        if (ex.isPresent()) {
            ex.get().setQuantity(ex.get().getQuantity() + qty);
            userItemRepository.save(ex.get());
        } else {
            UserItem ui = new UserItem();
            ui.setUser(user);
            ui.setItem(item);
            ui.setQuantity(qty);
            ui.setEnhanceLevel(enhance);
            ui.setIsEquipped(false);
            userItemRepository.save(ui);
        }
        return "Đã nhận vật phẩm vào kho";
    }
}