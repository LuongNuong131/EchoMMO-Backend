package com.echommo.service;

import com.echommo.entity.*;
import com.echommo.entity.Character;
import com.echommo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class InventoryService {

    @Autowired private UserItemRepository userItemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CharacterRepository characterRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private ItemRepository itemRepository;

    public List<UserItem> getMyInventory() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        Wallet wallet = user.getWallet();

        List<UserItem> inventory = userItemRepository.findByUser_UserIdOrderByIsEquippedDesc(user.getUserId());
        List<UserItem> displayList = new ArrayList<>(inventory);

        // Tạo item ảo từ Wallet (Gỗ/Đá) để hiển thị
        if (wallet.getWood() > 0) createVirtualItem(user, "Gỗ Sồi", wallet.getWood(), displayList);
        if (wallet.getStone() > 0) createVirtualItem(user, "Đá Cứng", wallet.getStone(), displayList);

        return displayList;
    }

    private void createVirtualItem(User user, String itemName, Integer quantity, List<UserItem> list) {
        Optional<Item> itemOpt = itemRepository.findByName(itemName);
        if (itemOpt.isPresent()) {
            UserItem dummy = new UserItem();
            dummy.setUserItemId(-new Random().nextInt(10000)); // ID âm để phân biệt
            dummy.setUser(user);
            dummy.setItem(itemOpt.get());
            dummy.setQuantity(quantity);
            dummy.setIsEquipped(false);
            dummy.setEnhanceLevel(0);
            list.add(dummy);
        }
    }

    @Transactional
    public String useItem(Integer userItemId) {
        UserItem item = userItemRepository.findById(userItemId).orElseThrow();
        if (!"CONSUMABLE".equals(item.getItem().getType())) throw new RuntimeException("Không dùng được");

        Character character = characterRepository.findByUser_UserId(item.getUser().getUserId()).orElseThrow();

        if (item.getItem().getHpBonus() > 0) {
            character.setHp(Math.min(character.getMaxHp(), character.getHp() + item.getItem().getHpBonus()));
        }

        if (item.getQuantity() > 1) {
            item.setQuantity(item.getQuantity() - 1);
            userItemRepository.save(item);
        } else {
            userItemRepository.delete(item);
        }
        characterRepository.save(character);
        return "Đã sử dụng " + item.getItem().getName();
    }

    @Transactional
    public String equipItem(Integer userItemId) {
        UserItem itemToEquip = userItemRepository.findById(userItemId).orElseThrow();
        User user = itemToEquip.getUser();
        Character character = characterRepository.findByUser_UserId(user.getUserId()).orElseThrow();

        if (itemToEquip.getIsEquipped()) return "Đã trang bị rồi";

        // Gỡ đồ cũ cùng loại
        String type = itemToEquip.getItem().getType();
        UserItem oldItem = userItemRepository.findByUser_UserIdAndItem_TypeAndIsEquippedTrue(user.getUserId(), type);

        if (oldItem != null) {
            oldItem.setIsEquipped(false);
            updateStats(character, oldItem.getItem(), -1); // Trừ chỉ số cũ
            userItemRepository.save(oldItem);
        }

        // Mặc đồ mới
        itemToEquip.setIsEquipped(true);
        updateStats(character, itemToEquip.getItem(), 1); // Cộng chỉ số mới

        userItemRepository.save(itemToEquip);
        characterRepository.save(character);

        return "Đã trang bị: " + itemToEquip.getItem().getName();
    }

    @Transactional
    public String unequipItem(Integer userItemId) {
        UserItem item = userItemRepository.findById(userItemId).orElseThrow();
        if (!item.getIsEquipped()) return "Chưa trang bị";

        Character character = characterRepository.findByUser_UserId(item.getUser().getUserId()).orElseThrow();

        item.setIsEquipped(false);
        updateStats(character, item.getItem(), -1); // Trừ chỉ số

        userItemRepository.save(item);
        characterRepository.save(character);
        return "Đã tháo: " + item.getItem().getName();
    }

    private void updateStats(Character c, Item i, int sign) {
        c.setBaseAtk(c.getBaseAtk() + (i.getAtkBonus() * sign));
        c.setBaseDef(c.getBaseDef() + (i.getDefBonus() * sign));
        c.setMaxHp(c.getMaxHp() + (i.getHpBonus() * sign));
        c.setBaseSpeed(c.getBaseSpeed() + (i.getSpeedBonus() * sign));
        c.setBaseCritRate(c.getBaseCritRate() + (i.getCritRateBonus() * sign));

        if (c.getHp() > c.getMaxHp()) c.setHp(c.getMaxHp());
    }

    public String enhanceItem(Integer id) { return "Tính năng đang bảo trì"; }
}