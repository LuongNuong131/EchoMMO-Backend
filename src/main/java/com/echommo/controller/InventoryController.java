package com.echommo.controller;

import com.echommo.entity.Character;
import com.echommo.entity.User;
import com.echommo.entity.UserItem;
import com.echommo.enums.SlotType;
import com.echommo.repository.CharacterRepository;
import com.echommo.repository.UserItemRepository;
import com.echommo.repository.UserRepository;
import com.echommo.service.InventoryService;
import com.echommo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
public class InventoryController {

    @Autowired private UserItemRepository userItemRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private CharacterRepository charRepo;
    @Autowired private InventoryService inventoryService;
    @Autowired private UserService userService;

    private Character getCurrentCharacter() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy User."));
        return charRepo.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Bạn chưa tạo nhân vật!"));
    }

    @GetMapping("/items")
    public ResponseEntity<List<UserItem>> getInventory() {
        return ResponseEntity.ok(inventoryService.getInventory(getCurrentCharacter().getCharId()));
    }

    @PostMapping("/expand")
    public ResponseEntity<?> expandInventory(Authentication auth) {
        User user = userService.getUserFromAuth(auth);
        try {
            User updatedUser = inventoryService.expandInventory(user);
            return ResponseEntity.ok("Mở rộng thành công! Tổng ô: " + updatedUser.getInventorySlots());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/equip/{userItemId}")
    @Transactional
    public ResponseEntity<?> equipItem(@PathVariable Long userItemId) { // [FIXED] Long
        try {
            Character character = getCurrentCharacter();

            UserItem newItem = userItemRepo.findById(userItemId)
                    .orElseThrow(() -> new RuntimeException("Item không tồn tại"));

            if (!newItem.getCharacter().getCharId().equals(character.getCharId())) {
                return ResponseEntity.badRequest().body("Vật phẩm không thuộc về nhân vật này!");
            }
            if (Boolean.TRUE.equals(newItem.getIsLocked())) {
                return ResponseEntity.badRequest().body("Vật phẩm đang bị khóa!");
            }

            SlotType slot = newItem.getItem().getSlotType();
            if (slot == null || slot == SlotType.CONSUMABLE || slot == SlotType.MATERIAL) {
                return ResponseEntity.badRequest().body("Không thể trang bị vật phẩm này!");
            }

            Optional<UserItem> currentEquip = userItemRepo.findEquippedItemBySlot(character.getCharId(), slot);
            if (currentEquip.isPresent()) {
                UserItem old = currentEquip.get();
                old.setIsEquipped(false);
                userItemRepo.save(old);
            }

            newItem.setIsEquipped(true);
            userItemRepo.save(newItem);
            return ResponseEntity.ok("Đã trang bị: " + newItem.getItem().getName());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/unequip/{userItemId}")
    @Transactional
    public ResponseEntity<?> unequipItem(@PathVariable Long userItemId) { // [FIXED] Long
        try {
            Character character = getCurrentCharacter();

            UserItem item = userItemRepo.findById(userItemId)
                    .orElseThrow(() -> new RuntimeException("Item không tồn tại"));

            if (!item.getCharacter().getCharId().equals(character.getCharId())) {
                return ResponseEntity.badRequest().body("Vật phẩm không thuộc về bạn!");
            }
            if (Boolean.TRUE.equals(item.getIsLocked())) {
                return ResponseEntity.badRequest().body("Vật phẩm đang bị khóa!");
            }

            item.setIsEquipped(false);
            userItemRepo.save(item);
            return ResponseEntity.ok("Đã tháo trang bị.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/use/{userItemId}")
    @Transactional
    public ResponseEntity<?> useItem(@PathVariable Long userItemId) { // [FIXED] Long
        try {
            Character character = getCurrentCharacter();

            UserItem uItem = userItemRepo.findById(userItemId)
                    .orElseThrow(() -> new RuntimeException("Item không tồn tại"));

            if (!uItem.getCharacter().getCharId().equals(character.getCharId())) {
                return ResponseEntity.badRequest().body("Lỗi sở hữu vật phẩm!");
            }

            if (uItem.getItem().getSlotType() != SlotType.CONSUMABLE) {
                return ResponseEntity.badRequest().body("Không thể sử dụng vật phẩm này!");
            }

            int heal = 50;
            if (uItem.getItem().getName().toLowerCase().contains("cao cấp")) heal = 200;

            character.setCurrentHp(Math.min(character.getMaxHp(), character.getCurrentHp() + heal));
            charRepo.save(character);

            uItem.setQuantity(uItem.getQuantity() - 1);
            if (uItem.getQuantity() <= 0) userItemRepo.delete(uItem);
            else userItemRepo.save(uItem);

            return ResponseEntity.ok("Đã dùng. HP hiện tại: " + character.getCurrentHp());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/repair/{userItemId}")
    @Transactional
    public ResponseEntity<?> repairItem(@PathVariable Long userItemId, Authentication auth) { // [FIXED] Long
        User user = userService.getUserFromAuth(auth);
        try {
            UserItem repairedItem = inventoryService.repairItem(user, userItemId);
            return ResponseEntity.ok("Sửa chữa thành công! Độ bền: " + repairedItem.getCurrentDurability());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}